package com.aqpseller.lulaapp.features.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.cita.ObtenerCitasUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.ObtenerSesionesCitaUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.MarcarTomaMedicamentoUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.ObtenerMedicamentosDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerMedicamentosDeHoyUseCase: ObtenerMedicamentosDeHoyUseCase,
    private val obtenerCitasUseCase: ObtenerCitasUseCase,
    private val obtenerSesionesCitaUseCase: ObtenerSesionesCitaUseCase,
    private val marcarTomaMedicamentoUseCase: MarcarTomaMedicamentoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual

            launch {
                obtenerMedicamentosDeHoyUseCase(sesionActual.espacioId).collect { medicamentos ->
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            medicamentos = medicamentos.map { m ->
                                MedicamentoUi(
                                    actividadId = m.actividadId,
                                    nombre = m.nombre,
                                    dosis = m.dosis,
                                    tomasHoy = m.tomas.map { t ->
                                        TomaUi(actividadId = m.actividadId, horario = t.horario, instruccion = t.instruccion, estado = t.estado)
                                    },
                                )
                            },
                        )
                    }
                }
            }

            launch {
                obtenerCitasUseCase(sesionActual.espacioId).collect { citas ->
                    val ahora = DateTimeUtils.ahoraEpochMillis()
                    val proximas = mutableListOf<CitaUi>()
                    for (cita in citas) {
                        if (!cita.activa) continue
                        val detalle = cita.detalle as? ActividadDetalle.Cita ?: continue
                        val item = if (detalle.esCurso) {
                            citaDeCursoUi(cita.id, cita.nombre, detalle)
                        } else {
                            citaPuntualUi(cita.id, cita.nombre, cita.estado, detalle, ahora)
                        }
                        if (item != null) proximas += item
                    }
                    _uiState.update { it.copy(proximasCitas = proximas.sortedBy { it.fechaHora }) }
                }
            }
        }
    }

    /** Cita puntual — igual que antes, se deja de mostrar apenas pasa su hora o se marca cumplida. */
    private fun citaPuntualUi(actividadId: String, nombre: String, estado: EstadoActividad, detalle: ActividadDetalle.Cita, ahora: Long): CitaUi? {
        if (estado == EstadoActividad.CONFIRMADO || detalle.fechaHora < ahora) return null
        return CitaUi(actividadId = actividadId, nombre = nombre, fechaHora = detalle.fechaHora, lugar = detalle.lugar)
    }

    /**
     * Cita de curso — antes usaba `detalle.fechaHora` (la fecha de la PRIMERA sesión nada más),
     * así que un curso ya empezado desaparecía de "Próximas citas" aunque le quedaran sesiones
     * pendientes (bug reportado por el usuario). Ahora se calcula de las sesiones reales:
     * próxima fecha = la primera sesión todavía `SIN_CONFIRMAR`; si ya no queda ninguna, el
     * curso está completo y se deja de mostrar acá.
     */
    private suspend fun citaDeCursoUi(actividadId: String, nombre: String, detalle: ActividadDetalle.Cita): CitaUi? {
        val sesiones = obtenerSesionesCitaUseCase(actividadId)
        val proximaPendiente = sesiones.filter { it.estado == EstadoActividad.SIN_CONFIRMAR }.minByOrNull { it.numeroSesion } ?: return null
        val cumplidas = sesiones.count { it.estado == EstadoActividad.CONFIRMADO }
        val total = detalle.cantidadSesionesTotal
        val progreso = if (total != null) "Van $cumplidas de $total sesiones" else "$cumplidas sesiones cumplidas"
        val fechaHoraSesion = DateTimeUtils.combinarFechaYHora(DateTimeUtils.epochDiasAEpochMillis(proximaPendiente.fecha), proximaPendiente.horario)
        return CitaUi(
            actividadId = actividadId,
            nombre = "$nombre (sesión ${proximaPendiente.numeroSesion}${total?.let { "/$it" } ?: ""})",
            fechaHora = fechaHoraSesion,
            lugar = detalle.lugar,
            esCurso = true,
            progresoTexto = progreso,
        )
    }

    fun marcarToma(actividadId: String, horario: String, estado: EstadoActividad) {
        viewModelScope.launch {
            marcarTomaMedicamentoUseCase(actividadId, horario, estado, sesionActual().usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
