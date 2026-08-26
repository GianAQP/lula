package com.aqpseller.lulaapp.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.codificarCodigoEspacioQr
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.espacio.CambiarEspacioActivoUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.CrearEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.EliminarEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.EliminarMiembroEspacioUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.GenerarCodigoInvitacionEspacioUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.InvitarAEspacioUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.ObtenerEspaciosDeUsuarioUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.ObtenerMiembrosEspacioUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.RenombrarEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.SalirDeEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Familia / Espacios" (CONFIGURACIÓN en el menú "⋮") — crear espacios Familia, ver quién es
 * miembro de cada uno y cambiar el espacio activo. Un usuario puede tener varias Familias (la
 * que formó, la de sus padres, la de su pareja) — cada una es un `Espacio` independiente; la
 * capa de datos/sync ya lo soporta sin cambios, esta pantalla es la que antes se quedaba solo
 * con la primera. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@HiltViewModel
class FamiliaViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerEspaciosDeUsuarioUseCase: ObtenerEspaciosDeUsuarioUseCase,
    private val obtenerMiembrosEspacioUseCase: ObtenerMiembrosEspacioUseCase,
    private val crearEspacioFamiliaUseCase: CrearEspacioFamiliaUseCase,
    private val cambiarEspacioActivoUseCase: CambiarEspacioActivoUseCase,
    private val renombrarEspacioFamiliaUseCase: RenombrarEspacioFamiliaUseCase,
    private val eliminarEspacioFamiliaUseCase: EliminarEspacioFamiliaUseCase,
    private val invitarAEspacioUseCase: InvitarAEspacioUseCase,
    private val generarCodigoInvitacionEspacioUseCase: GenerarCodigoInvitacionEspacioUseCase,
    private val salirDeEspacioFamiliaUseCase: SalirDeEspacioFamiliaUseCase,
    private val eliminarMiembroEspacioUseCase: EliminarMiembroEspacioUseCase,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamiliaUiState())
    val uiState: StateFlow<FamiliaUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null
    private var nombreUsuarioActual: String = "Tú"
    private var jobCodigoQr: Job? = null
    private var jobMiembros: Job? = null

    init {
        viewModelScope.launch {
            usuarioRepository.observarUsuario().collect { usuario ->
                nombreUsuarioActual = usuario?.nombrePreferido ?: "Tú"
                _uiState.update { it.copy(cuentaVinculada = !usuario?.correo.isNullOrBlank()) }
            }
        }
        viewModelScope.launch {
            val sesionActual = sesionActual()
            obtenerEspaciosDeUsuarioUseCase(sesionActual.usuarioId).collectLatest { espacios ->
                val familias = espacios.filter { it.tipo == TipoEspacio.FAMILIA }
                _uiState.update { estado ->
                    estado.copy(
                        cargando = false,
                        espacios = espacios.map { espacio ->
                            EspacioUi(
                                id = espacio.id,
                                nombre = espacio.nombre,
                                esFamilia = espacio.tipo == TipoEspacio.FAMILIA,
                                esActivo = espacio.id == sesionActual.espacioId,
                            )
                        },
                        familias = familias.map { FamiliaResumenUi(id = it.id, nombre = it.nombre) },
                        // Si la Familia que estaba viendo se eliminó/dejé de ser miembro (ej. la
                        // borró otro admin), cierro el detalle en vez de dejarlo mostrando datos
                        // de un espacio que ya no existe para mí.
                        familiaSeleccionadaId = estado.familiaSeleccionadaId?.takeIf { id -> familias.any { it.id == id } },
                    )
                }
            }
        }
    }

    /** Ver/administrar una Familia puntual — independiente del "espacio activo" de arriba, así
     * no hace falta cambiar de espacio de trabajo solo para invitar a alguien a otra Familia. */
    fun seleccionarFamilia(espacioId: String, nombre: String) {
        jobMiembros?.cancel()
        _uiState.update {
            it.copy(
                familiaSeleccionadaId = espacioId,
                nombreEspacioFamilia = nombre,
                mostrarFormularioRenombrar = false,
                mostrarFormularioInvitar = false,
            )
        }
        jobMiembros = viewModelScope.launch {
            obtenerMiembrosEspacioUseCase(espacioId).collect { miembros ->
                val miUsuarioId = sesionActual().usuarioId
                _uiState.update { estado ->
                    estado.copy(
                        soyAdmin = miembros.find { it.usuarioId == miUsuarioId }?.rol == RolEnEspacio.ADMIN,
                        miembros = miembros.map { miembro ->
                            val esUnoMismo = miembro.usuarioId == miUsuarioId
                            val nombreMiembro = if (esUnoMismo) nombreUsuarioActual else miembro.nombre ?: "Alguien"
                            MiembroUi(
                                usuarioId = miembro.usuarioId,
                                firebaseUid = miembro.firebaseUid,
                                nombre = nombreMiembro,
                                rol = if (miembro.rol == RolEnEspacio.ADMIN) "Admin" else "Miembro",
                                esUnoMismo = esUnoMismo,
                            )
                        },
                    )
                }
            }
        }
    }

    fun cerrarFamiliaSeleccionada() {
        jobMiembros?.cancel()
        _uiState.update { it.copy(familiaSeleccionadaId = null, miembros = emptyList()) }
    }

    fun mostrarFormularioCrear() {
        _uiState.update { it.copy(mostrarFormularioCrear = true) }
    }

    fun ocultarFormularioCrear() {
        _uiState.update { it.copy(mostrarFormularioCrear = false) }
    }

    fun crearEspacioFamilia(nombre: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            crearEspacioFamiliaUseCase(nombre, sesionActual().usuarioId)
            _uiState.update { it.copy(mostrarFormularioCrear = false, espacioCambiado = true) }
        }
    }

    fun seleccionarEspacio(espacioId: String) {
        viewModelScope.launch {
            cambiarEspacioActivoUseCase(espacioId)
            _uiState.update { it.copy(espacioCambiado = true) }
        }
    }

    fun mostrarFormularioRenombrar() {
        _uiState.update { it.copy(mostrarFormularioRenombrar = true) }
    }

    fun ocultarFormularioRenombrar() {
        _uiState.update { it.copy(mostrarFormularioRenombrar = false) }
    }

    fun renombrarEspacioFamilia(nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        val espacioId = _uiState.value.familiaSeleccionadaId ?: return
        viewModelScope.launch {
            renombrarEspacioFamiliaUseCase(espacioId, nuevoNombre, sesionActual().usuarioId)
            _uiState.update { it.copy(mostrarFormularioRenombrar = false, nombreEspacioFamilia = nuevoNombre) }
        }
    }

    fun eliminarEspacioFamilia() {
        val espacioId = _uiState.value.familiaSeleccionadaId ?: return
        viewModelScope.launch {
            eliminarEspacioFamiliaUseCase(espacioId, sesionActual().usuarioId)
            cerrarFamiliaSeleccionada()
        }
    }

    /** Salir del espacio yo mismo — el contenido ya creado ahí se queda tal cual. */
    fun salirDeEspacioFamilia() {
        val espacioId = _uiState.value.familiaSeleccionadaId ?: return
        viewModelScope.launch {
            salirDeEspacioFamiliaUseCase(espacioId, sesionActual().usuarioId)
            cerrarFamiliaSeleccionada()
        }
    }

    /** Quitar a otra persona — solo tiene efecto si yo soy admin (verificado también del lado
     * del servidor, ver `firestore.rules`). */
    fun eliminarMiembro(miembro: MiembroUi) {
        val espacioId = _uiState.value.familiaSeleccionadaId ?: return
        viewModelScope.launch {
            eliminarMiembroEspacioUseCase(espacioId, miembro.usuarioId, miembro.firebaseUid, sesionActual().usuarioId)
        }
    }

    fun mostrarFormularioInvitar() {
        _uiState.update { it.copy(mostrarFormularioInvitar = true) }
    }

    fun ocultarFormularioInvitar() {
        _uiState.update { it.copy(mostrarFormularioInvitar = false) }
    }

    fun invitar(contacto: String) {
        if (contacto.isBlank()) return
        val espacioId = _uiState.value.familiaSeleccionadaId ?: return
        val nombreEspacio = _uiState.value.nombreEspacioFamilia
        viewModelScope.launch {
            invitarAEspacioUseCase(sesionActual().usuarioId, espacioId, nombreEspacio, contacto)
            _uiState.update { it.copy(mostrarFormularioInvitar = false, mostrarInvitacionEnviada = true) }
        }
    }

    fun ocultarInvitacionEnviada() {
        _uiState.update { it.copy(mostrarInvitacionEnviada = false) }
    }

    /** Genera un código y lo renueva solo, mientras el diálogo siga abierto, un poco antes de
     * que cada uno venza — así siempre hay un QR vigente en pantalla. */
    fun mostrarCodigoQr() {
        val espacioId = _uiState.value.familiaSeleccionadaId ?: return
        val nombreEspacio = _uiState.value.nombreEspacioFamilia
        _uiState.update { it.copy(mostrarCodigoQr = true) }
        jobCodigoQr?.cancel()
        jobCodigoQr = viewModelScope.launch {
            while (true) {
                val codigo = runCatching { generarCodigoInvitacionEspacioUseCase(espacioId, nombreEspacio) }.getOrNull()
                if (codigo == null) {
                    _uiState.update { it.copy(mostrarCodigoQr = false, codigoQrTexto = null) }
                    return@launch
                }
                _uiState.update { it.copy(codigoQrTexto = codificarCodigoEspacioQr(codigo.codigoId)) }
                delay((codigo.expiraEn - System.currentTimeMillis()).coerceAtLeast(1_000))
            }
        }
    }

    fun ocultarCodigoQr() {
        jobCodigoQr?.cancel()
        _uiState.update { it.copy(mostrarCodigoQr = false, codigoQrTexto = null) }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
