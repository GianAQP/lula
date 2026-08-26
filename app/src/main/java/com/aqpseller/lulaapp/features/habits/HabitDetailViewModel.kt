package com.aqpseller.lulaapp.features.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.actividad.EliminarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHistorialHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.PausarReanudarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DIAS_HISTORIAL = 30

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val obtenerHistorialHabitoUseCase: ObtenerHistorialHabitoUseCase,
    private val pausarReanudarActividadUseCase: PausarReanudarActividadUseCase,
    private val eliminarActividadUseCase: EliminarActividadUseCase,
) : ViewModel() {

    val actividadId: String = checkNotNull(savedStateHandle["actividadId"])

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

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
            val actividad = obtenerDetalleActividadUseCase(actividadId) ?: return@launch
            val detalle = actividad.detalle as? ActividadDetalle.Habito
            val racha = obtenerHistorialHabitoUseCase.calcularRacha(actividadId)
            val historial = obtenerHistorialHabitoUseCase.ultimosDias(actividadId, DIAS_HISTORIAL)
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = actividad.nombre,
                    momentoDelDia = detalle?.momentoDelDia,
                    racha = racha,
                    diasHistorial30 = historial.map { dia -> dia.estado == EstadoActividad.CONFIRMADO },
                    activa = actividad.activa,
                )
            }
        }
    }

    fun pausarOReanudar() {
        viewModelScope.launch {
            pausarReanudarActividadUseCase(actividadId, !_uiState.value.activa, sesionActual().usuarioId)
            _uiState.update { it.copy(activa = !it.activa) }
        }
    }

    fun eliminar() {
        viewModelScope.launch {
            eliminarActividadUseCase(actividadId, sesionActual().usuarioId)
            _uiState.update { it.copy(eliminado = true) }
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
