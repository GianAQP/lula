package com.aqpseller.lulaapp.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.actividad.MarcarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerTareasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksListViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerTareasUseCase: ObtenerTareasUseCase,
    private val marcarActividadUseCase: MarcarActividadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksListUiState())
    val uiState: StateFlow<TasksListUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            obtenerTareasUseCase(sesionActual.espacioId)
                .map { tareas ->
                    tareas.map { tarea ->
                        val detalle = tarea.detalle as? ActividadDetalle.Tarea
                        TareaListItemUi(
                            id = tarea.id,
                            nombre = tarea.nombre,
                            fechaLimite = detalle?.fechaLimite,
                            estado = tarea.estado,
                            importante = detalle?.importante ?: false,
                            urgente = detalle?.urgente ?: false,
                        )
                    }
                }
                .collect { tareas -> _uiState.value = TasksListUiState(cargando = false, tareas = tareas) }
        }
    }

    fun alternarCompletada(tareaId: String, estadoActual: EstadoActividad) {
        val nuevoEstado = if (estadoActual == EstadoActividad.CONFIRMADO) EstadoActividad.SIN_CONFIRMAR else EstadoActividad.CONFIRMADO
        viewModelScope.launch {
            marcarActividadUseCase(tareaId, nuevoEstado, sesionActual().usuarioId)
        }
    }

    /**
     * Si `init` todavía no terminó de resolver la sesión (toque muy rápido apenas se abre la
     * pantalla), no descarta la acción en silencio — la resuelve ahí mismo. Antes un
     * `sesion ?: return` fuera de una corrutina podía perder el toque sin avisar. Ver
     * `Plan/08-decisiones-tecnicas.md`.
     */
    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
