package com.aqpseller.lulaapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.decodificarContactoQr
import com.aqpseller.lulaapp.core.utils.decodificarListaQr
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.carecircle.ObtenerSolicitudesRecibidasUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.ImportarListaDesdeQrUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopBarStatsUiState(
    val racha: Int = 0,
    val gastosHoyTotal: Double = 0.0,
    val solicitudesPendientes: Int = 0,
    /** Null = espacio Personal — ver "banda de espacio activo", `Plan/08-decisiones-tecnicas.md`. */
    val nombreEspacioActivo: String? = null,
    val mensaje: String? = null,
    /** Cuando escaneo el código de contacto de alguien — la UI lo copia al portapapeles. */
    val correoParaCopiar: String? = null,
)

/**
 * Racha, gastos de hoy e invitaciones pendientes — viven en `LulaTopBar` (mismo nivel que el
 * menú "⋮") para que se vean en cualquier pantalla, no solo en Hoy, a pedido del usuario.
 */
@HiltViewModel
class TopBarStatsViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
    private val obtenerSolicitudesRecibidasUseCase: ObtenerSolicitudesRecibidasUseCase,
    private val espacioRepository: EspacioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val importarListaDesdeQrUseCase: ImportarListaDesdeQrUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopBarStatsUiState())
    val uiState: StateFlow<TopBarStatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            refrescarRacha(sesion.espacioId)
            refrescarEspacioActivo(sesion.espacioId, sesion.usuarioId)

            launch {
                obtenerBalanceMesUseCase(sesion.espacioId, DateTimeUtils.inicioDeHoyEpochMillis(), DateTimeUtils.finDeHoyEpochMillis())
                    .collect { movimientos ->
                        val gastos = movimientos.filter { it.tipo == TipoMovimientoFinanciero.EGRESO }.sumOf { it.monto }
                        _uiState.update { it.copy(gastosHoyTotal = gastos) }
                    }
            }

            launch {
                // `observarPendientesPara` filtra por correo (no por usuarioId) — vacío hasta
                // que la cuenta esté vinculada con Google. Ver `Plan/12-firebase-auth-y-sync.md`.
                val correo = usuarioRepository.observarUsuario().first()?.correo ?: ""
                obtenerSolicitudesRecibidasUseCase(correo).collect { solicitudes ->
                    _uiState.update { it.copy(solicitudesPendientes = solicitudes.size) }
                }
            }
        }
    }

    /** Botón global de escanear (barra superior, visible en toda la app) — detecta solo qué
     * tipo de código de Lula es y actúa: importa una Lista, o deja el correo de un contacto
     * listo para pegar en "Compartir"/"Invitar". Ver `Plan/12-firebase-auth-y-sync.md`. */
    fun escanear(qrTexto: String) {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val lista = decodificarListaQr(qrTexto)
            if (lista != null) {
                importarListaDesdeQrUseCase(qrTexto, sesion.espacioId, sesion.usuarioId)
                _uiState.update { it.copy(mensaje = "Lista \"${lista.nombre}\" importada ✅") }
                return@launch
            }
            val contacto = decodificarContactoQr(qrTexto)
            if (contacto != null) {
                _uiState.update {
                    it.copy(
                        correoParaCopiar = contacto.correo,
                        mensaje = "Correo de ${contacto.nombre} copiado — pégalo en \"Compartir\" o \"Invitar\"",
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(mensaje = "Ese código no es de Lula") }
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    fun correoCopiado() {
        _uiState.update { it.copy(correoParaCopiar = null) }
    }

    /**
     * La racha no tiene una fuente reactiva (`calcularRachaActual` es un cálculo puntual, no
     * un `Flow`) — se vuelve a pedir cada vez que cambia de pantalla (ver `LulaTopBar`), para
     * que se note después de "Cerrar mi día" sin tener que reabrir la app.
     */
    fun refrescar() {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            refrescarRacha(sesion.espacioId)
            refrescarEspacioActivo(sesion.espacioId, sesion.usuarioId)
        }
    }

    private suspend fun refrescarRacha(espacioId: String) {
        val racha = obtenerProgresoDeHoyUseCase.calcularRachaActual(espacioId)
        _uiState.update { it.copy(racha = racha) }
    }

    private suspend fun refrescarEspacioActivo(espacioId: String, usuarioId: String) {
        val personal = espacioRepository.obtenerEspacioPersonal(usuarioId)
        val nombre = if (personal != null && personal.id != espacioId) {
            espacioRepository.obtenerEspacioSiEsMiembro(espacioId, usuarioId)?.nombre
        } else {
            null
        }
        _uiState.update { it.copy(nombreEspacioActivo = nombre) }
    }
}
