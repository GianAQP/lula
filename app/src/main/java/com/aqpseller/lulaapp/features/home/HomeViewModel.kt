package com.aqpseller.lulaapp.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.usecase.actividad.MarcarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerActividadesDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerActividadesDeHoyUseCase: ObtenerActividadesDeHoyUseCase,
    private val marcarActividadUseCase: MarcarActividadUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            refrescarProgresoDelDia(sesionActual)

            launch {
                obtenerActividadesDeHoyUseCase(sesionActual.espacioId).collect { actividades ->
                    _uiState.update { estado ->
                        estado.copy(
                            cargando = false,
                            actividadesManana = actividades.aUi(TipoActividad.HABITO, MomentoDelDia.MANANA),
                            actividadesTarde = actividades.aUi(TipoActividad.HABITO, MomentoDelDia.TARDE),
                            actividadesNoche = actividades.aUi(TipoActividad.HABITO, MomentoDelDia.NOCHE),
                            tareasDeHoy = actividades
                                .filter { it.tipo == TipoActividad.TAREA && esTareaDeHoyOVencida(it.detalle) }
                                .map { ActividadUi(id = it.id, nombre = it.nombre, estado = it.estado) },
                        )
                    }
                }
            }

            launch {
                obtenerBalanceMesUseCase(
                    sesionActual.espacioId,
                    DateTimeUtils.inicioDeHoyEpochMillis(),
                    DateTimeUtils.finDeHoyEpochMillis(),
                ).collect { movimientos ->
                    val totalGastos = movimientos
                        .filter { it.tipo == TipoMovimientoFinanciero.EGRESO }
                        .sumOf { it.monto }
                    _uiState.update { it.copy(gastosHoyTotal = totalGastos) }
                }
            }
        }
    }

    fun marcarActividad(actividadId: String, estado: EstadoActividad) {
        val sesionActual = sesion ?: return
        viewModelScope.launch {
            marcarActividadUseCase(actividadId, estado, sesionActual.usuarioId)
        }
    }

    private suspend fun refrescarProgresoDelDia(sesionActual: SesionActual) {
        val racha = obtenerProgresoDeHoyUseCase.calcularRachaActual(sesionActual.espacioId)
        val registroHoy = obtenerProgresoDeHoyUseCase.registroDeHoy(sesionActual.espacioId)
        _uiState.update { it.copy(racha = racha, diaYaCerrado = registroHoy != null) }
    }

    private fun List<com.aqpseller.lulaapp.domain.model.Actividad>.aUi(
        tipo: TipoActividad,
        momento: MomentoDelDia,
    ) = filter { it.tipo == tipo && it.momentoDelDia == momento }
        .map { ActividadUi(id = it.id, nombre = it.nombre, estado = it.estado) }

    /** Tareas sin fecha límite o con fecha límite hoy/vencida (ver `08-decisiones-tecnicas.md`). */
    private fun esTareaDeHoyOVencida(detalle: ActividadDetalle?): Boolean {
        val tarea = detalle as? ActividadDetalle.Tarea ?: return false
        val fechaLimite = tarea.fechaLimite ?: return true
        return fechaLimite <= DateTimeUtils.finDeHoyEpochMillis()
    }
}
