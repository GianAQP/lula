package com.aqpseller.lulaapp.features.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerRutinasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutinesListViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerRutinasUseCase: ObtenerRutinasUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutinesListUiState())
    val uiState: StateFlow<RoutinesListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            obtenerRutinasUseCase(sesion.espacioId)
                .map { rutinas -> rutinas.map { it.aUi() } }
                .collect { rutinas -> _uiState.value = RoutinesListUiState(cargando = false, rutinas = rutinas) }
        }
    }

    private suspend fun Actividad.aUi(): RutinaListItemUi {
        val detalle = detalle as? ActividadDetalle.Rutina
        val incluidas = detalle?.actividadesIncluidasIds.orEmpty()
        val completadas = incluidas.count { id -> obtenerDetalleActividadUseCase(id)?.estado == EstadoActividad.CONFIRMADO }
        return RutinaListItemUi(
            id = id,
            nombre = nombre,
            momentoDelDia = detalle?.momentoDelDia ?: momentoDelDia ?: MomentoDelDia.MANANA,
            completadas = completadas,
            total = incluidas.size,
        )
    }
}
