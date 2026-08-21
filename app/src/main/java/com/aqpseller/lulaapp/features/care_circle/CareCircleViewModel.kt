package com.aqpseller.lulaapp.features.care_circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.carecircle.AceptarSolicitudCompartirUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.CancelarSolicitudCompartirUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.ObtenerSolicitudesEnviadasUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.ObtenerSolicitudesRecibidasUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.RechazarSolicitudCompartirUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.SincronizarSolicitudesRecibidasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CareCircleViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val usuarioRepository: UsuarioRepository,
    private val obtenerSolicitudesEnviadasUseCase: ObtenerSolicitudesEnviadasUseCase,
    private val obtenerSolicitudesRecibidasUseCase: ObtenerSolicitudesRecibidasUseCase,
    private val cancelarSolicitudCompartirUseCase: CancelarSolicitudCompartirUseCase,
    private val aceptarSolicitudCompartirUseCase: AceptarSolicitudCompartirUseCase,
    private val rechazarSolicitudCompartirUseCase: RechazarSolicitudCompartirUseCase,
    private val sincronizarSolicitudesRecibidasUseCase: SincronizarSolicitudesRecibidasUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CareCircleUiState())
    val uiState: StateFlow<CareCircleUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null
    private var recibidasCache: List<SolicitudCompartir> = emptyList()

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            val correo = usuarioRepository.observarUsuario().first()?.correo

            launch {
                obtenerSolicitudesEnviadasUseCase(sesionActual.usuarioId).collect { solicitudes ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            enviadas = solicitudes.map { s ->
                                SolicitudEnviadaUi(id = s.id, contacto = s.para, elemento = s.contexto, tipo = s.tipo, permiso = s.permisos, estado = s.estado)
                            },
                        )
                    }
                }
            }

            launch {
                obtenerSolicitudesRecibidasUseCase(correo ?: "").collect { solicitudes ->
                    recibidasCache = solicitudes
                    _uiState.update {
                        it.copy(
                            recibidas = solicitudes.map { s ->
                                SolicitudRecibidaUi(id = s.id, deNombre = s.deNombre, elemento = s.contexto, tipo = s.tipo, permiso = s.permisos)
                            },
                        )
                    }
                }
            }

            // Escucha Firestore mientras esta pantalla esté abierta — se cancela sola al
            // cerrarla. No-op si la cuenta todavía no está vinculada (correo en blanco).
            launch {
                sincronizarSolicitudesRecibidasUseCase(sesionActual.usuarioId, correo ?: "")
            }
        }
    }

    fun cancelar(solicitudId: String) {
        viewModelScope.launch {
            cancelarSolicitudCompartirUseCase(solicitudId, sesionActual().usuarioId)
        }
    }

    fun aceptar(solicitudId: String) {
        viewModelScope.launch {
            val solicitud = recibidasCache.find { it.id == solicitudId } ?: return@launch
            aceptarSolicitudCompartirUseCase(solicitud, sesionActual().usuarioId)
        }
    }

    fun rechazar(solicitudId: String) {
        viewModelScope.launch {
            val solicitud = recibidasCache.find { it.id == solicitudId } ?: return@launch
            rechazarSolicitudCompartirUseCase(solicitud, sesionActual().usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
