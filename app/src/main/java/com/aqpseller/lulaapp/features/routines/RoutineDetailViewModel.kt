package com.aqpseller.lulaapp.features.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.actividad.EliminarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.MarcarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val marcarActividadUseCase: MarcarActividadUseCase,
    private val eliminarActividadUseCase: EliminarActividadUseCase,
) : ViewModel() {

    val actividadId: String = checkNotNull(savedStateHandle["actividadId"])

    private val _uiState = MutableStateFlow(RoutineDetailUiState())
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        cargar()
    }

    /**
     * Navigation Compose reutiliza esta misma instancia del ViewModel al volver de Editar
     * (la pantalla no se recrea, solo se recompone) — sin volver a llamar `cargar()`, el
     * `uiState` se quedaba con la foto de antes de editar hasta salir y volver a entrar.
     */
    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            val rutina = obtenerDetalleActividadUseCase(actividadId) ?: return@launch
            val detalle = rutina.detalle as? ActividadDetalle.Rutina ?: return@launch
            val items = detalle.actividadesIncluidasIds.mapNotNull { id ->
                obtenerDetalleActividadUseCase(id)?.let { RutinaItemUi(id = it.id, nombre = it.nombre, estado = it.estado) }
            }
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = rutina.nombre,
                    momentoDelDia = detalle.momentoDelDia,
                    items = items,
                )
            }
        }
    }

    fun marcarItem(itemId: String, marcado: Boolean) {
        viewModelScope.launch {
            marcarActividadUseCase(
                itemId,
                if (marcado) EstadoActividad.CONFIRMADO else EstadoActividad.SIN_CONFIRMAR,
                sesionActual().usuarioId,
            )
            cargar()
        }
    }

    fun marcarTodaCompleta() {
        viewModelScope.launch {
            val usuarioId = sesionActual().usuarioId
            _uiState.value.items.forEach { item ->
                marcarActividadUseCase(item.id, EstadoActividad.CONFIRMADO, usuarioId)
            }
            cargar()
        }
    }

    fun eliminar() {
        viewModelScope.launch {
            eliminarActividadUseCase(actividadId, sesionActual().usuarioId)
            _uiState.update { it.copy(eliminada = true) }
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
