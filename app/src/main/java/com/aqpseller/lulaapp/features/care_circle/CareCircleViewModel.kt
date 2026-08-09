package com.aqpseller.lulaapp.features.care_circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.carecircle.CancelarSolicitudCompartirUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.ObtenerSolicitudesEnviadasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CareCircleViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerSolicitudesEnviadasUseCase: ObtenerSolicitudesEnviadasUseCase,
    private val cancelarSolicitudCompartirUseCase: CancelarSolicitudCompartirUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CareCircleUiState())
    val uiState: StateFlow<CareCircleUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            obtenerSolicitudesEnviadasUseCase(sesionActual.usuarioId).collect { solicitudes ->
                _uiState.update {
                    it.copy(
                        cargando = false,
                        enviadas = solicitudes.map { s ->
                            SolicitudEnviadaUi(id = s.id, contacto = s.para, elemento = s.contexto, permiso = s.permisos, estado = s.estado)
                        },
                    )
                }
            }
        }
    }

    fun cancelar(solicitudId: String) {
        viewModelScope.launch {
            cancelarSolicitudCompartirUseCase(solicitudId, sesionActual().usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
