package com.aqpseller.lulaapp.features.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.actividad.ActualizarRutinaUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.CrearRutinaUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHabitosUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerTareasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RutinaFormInicial(
    val nombre: String,
    val momentoDelDia: MomentoDelDia,
    val actividadesIncluidasIds: List<String>,
)

@HiltViewModel
class CrearRutinaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val obtenerHabitosUseCase: ObtenerHabitosUseCase,
    private val obtenerTareasUseCase: ObtenerTareasUseCase,
    private val crearRutinaUseCase: CrearRutinaUseCase,
    private val actualizarRutinaUseCase: ActualizarRutinaUseCase,
) : ViewModel() {

    private val actividadId: String? = savedStateHandle.get<String>("actividadId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = actividadId != null

    private val _formInicial = MutableStateFlow<RutinaFormInicial?>(null)
    val formInicial: StateFlow<RutinaFormInicial?> = _formInicial.asStateFlow()

    private val _actividadesDisponibles = MutableStateFlow<List<Actividad>>(emptyList())
    val actividadesDisponibles: StateFlow<List<Actividad>> = _actividadesDisponibles.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual

            combine(obtenerHabitosUseCase(sesionActual.espacioId), obtenerTareasUseCase(sesionActual.espacioId)) { habitos, tareas ->
                // Una Tarea ya completada no tiene sentido agruparla en una rutina hacia
                // adelante — sin este filtro, esta lista de selección crecía para siempre con
                // cada tarea alguna vez creada, hecha o no (reportado por el usuario, ver
                // `08-decisiones-tecnicas.md`). Hábito no se filtra: es recurrente, "ya se
                // cumplió hoy" no lo descalifica de una rutina que se repite mañana.
                habitos + tareas.filter { it.estado != EstadoActividad.CONFIRMADO }
            }.collect { disponibles -> _actividadesDisponibles.value = disponibles }
        }
        val id = actividadId
        if (id != null) {
            viewModelScope.launch {
                val actividad = obtenerDetalleActividadUseCase(id)
                val detalle = actividad?.detalle as? ActividadDetalle.Rutina
                if (actividad != null && detalle != null) {
                    _formInicial.value = RutinaFormInicial(
                        nombre = actividad.nombre,
                        momentoDelDia = detalle.momentoDelDia,
                        actividadesIncluidasIds = detalle.actividadesIncluidasIds,
                    )
                }
            }
        }
    }

    fun guardar(nombre: String, momentoDelDia: MomentoDelDia, actividadesIncluidasIds: List<String>) {
        if (nombre.isBlank() || actividadesIncluidasIds.isEmpty()) return
        viewModelScope.launch {
            val sesionActual = sesionActual()
            val id = actividadId
            if (id != null) {
                actualizarRutinaUseCase(id, sesionActual.usuarioId, nombre, momentoDelDia, actividadesIncluidasIds)
            } else {
                crearRutinaUseCase(
                    espacioId = sesionActual.espacioId,
                    propietario = sesionActual.usuarioId,
                    nombre = nombre,
                    momentoDelDia = momentoDelDia,
                    actividadesIncluidasIds = actividadesIncluidasIds,
                )
            }
            _guardado.value = true
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
