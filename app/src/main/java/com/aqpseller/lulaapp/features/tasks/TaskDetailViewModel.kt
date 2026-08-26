package com.aqpseller.lulaapp.features.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.ui.CompartirPorQrController
import com.aqpseller.lulaapp.core.ui.CompartirQrEstado
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.usecase.actividad.EliminarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.MarcarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.CompartirActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val marcarActividadUseCase: MarcarActividadUseCase,
    private val eliminarActividadUseCase: EliminarActividadUseCase,
    private val compartirActividadUseCase: CompartirActividadUseCase,
    private val compartirPorQrController: CompartirPorQrController,
) : ViewModel() {

    val actividadId: String = checkNotNull(savedStateHandle["actividadId"])

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _solicitudEnviada = MutableStateFlow(false)
    val solicitudEnviada: StateFlow<Boolean> = _solicitudEnviada.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            cargar()
        }
    }

    /**
     * Navigation Compose reutiliza esta misma instancia del ViewModel al volver de Editar
     * (la pantalla no se recrea, solo se recompone) — sin volver a llamar `cargar()`, el
     * `uiState` se quedaba con la foto de antes de editar hasta salir y volver a entrar.
     */
    fun recargar() {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val actividad = obtenerDetalleActividadUseCase(actividadId) ?: return
        val detalle = actividad.detalle as? ActividadDetalle.Tarea
        val nombreVinculada = detalle?.actividadVinculadaId?.let { obtenerDetalleActividadUseCase(it)?.nombre }
        _uiState.update {
            it.copy(
                cargando = false,
                nombre = actividad.nombre,
                fechaLimite = detalle?.fechaLimite,
                importante = detalle?.importante ?: false,
                urgente = detalle?.urgente ?: false,
                estado = actividad.estado,
                fechaCompletado = actividad.fechaCompletado,
                recurrencia = detalle?.recurrencia ?: RecurrenciaTarea.SIN_REPETIR,
                nombreActividadVinculada = nombreVinculada,
            )
        }
    }

    /**
     * Vuelve a leer la actividad después de marcar, en vez de asumir el estado localmente —
     * si es una Tarea recurrente, `marcarActividadUseCase` la deja `SIN_CONFIRMAR` de nuevo con
     * la fecha siguiente, y un estado optimista local mostraría "Completada" incorrectamente.
     */
    fun alternarCompletada() {
        val nuevoEstado = if (_uiState.value.estado == EstadoActividad.CONFIRMADO) {
            EstadoActividad.SIN_CONFIRMAR
        } else {
            EstadoActividad.CONFIRMADO
        }
        viewModelScope.launch {
            marcarActividadUseCase(actividadId, nuevoEstado, sesionActual().usuarioId)
            cargar()
        }
    }

    fun eliminar() {
        viewModelScope.launch {
            eliminarActividadUseCase(actividadId, sesionActual().usuarioId)
            _uiState.update { it.copy(eliminada = true) }
        }
    }

    fun compartir(contacto: String, permiso: PermisoCompartir) {
        viewModelScope.launch {
            compartirActividadUseCase(sesionActual().usuarioId, actividadId, _uiState.value.nombre, contacto, permiso)
            _solicitudEnviada.value = true
        }
    }

    fun solicitudEnviadaMostrada() {
        _solicitudEnviada.value = false
    }

    val estadoCompartirQr: StateFlow<CompartirQrEstado> = compartirPorQrController.estado

    fun generarCodigoQr(permiso: PermisoCompartir) {
        viewModelScope.launch {
            val usuarioId = sesionActual().usuarioId
            compartirPorQrController.generar(viewModelScope, usuarioId, actividadId, TipoActividad.TAREA, _uiState.value.nombre, permiso)
        }
    }

    fun ocultarCodigoQr() = compartirPorQrController.ocultar()

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
