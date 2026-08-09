package com.aqpseller.lulaapp.features.finances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun MovimientoFinanciero.aUi() = MovimientoUi(
    id = id,
    tipo = tipo,
    categoria = categoria,
    monto = monto,
    descripcion = descripcion,
    fecha = fecha,
)

/** Historial navegable mes a mes — a propósito es un ViewModel aparte de `FinancesViewModel`,
 * que se queda siempre fijo en el mes en curso (ver `02-pantallas.md`: el widget "Este mes" de
 * Finanzas no debe moverse cuando el usuario navega el historial a otro mes). */
@HiltViewModel
class FinancesHistoryViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancesHistoryUiState(mesVisible = DateTimeUtils.hoy()))
    val uiState: StateFlow<FinancesHistoryUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            cargar()
        }
    }

    fun mesAnterior() = cambiarMes(-1)
    fun mesSiguiente() = cambiarMes(1)

    fun irAHoy() {
        _uiState.update { it.copy(mesVisible = DateTimeUtils.hoy(), modoRango = false) }
        viewModelScope.launch { cargar() }
    }

    /** Rango de fechas elegido a mano — para saber "cuánto gasté/gané entre estas dos fechas",
     * más allá de un mes calendario completo. Ver `Plan/08-decisiones-tecnicas.md`. */
    fun activarRangoPersonalizado(desde: Long, hasta: Long) {
        _uiState.update { it.copy(modoRango = true, fechaDesde = desde, fechaHasta = hasta) }
        viewModelScope.launch { cargar() }
    }

    fun volverAModoMes() {
        _uiState.update { it.copy(modoRango = false) }
        viewModelScope.launch { cargar() }
    }

    private fun cambiarMes(delta: Int) {
        _uiState.update { it.copy(mesVisible = DateTimeUtils.agregarMeses(it.mesVisible, delta), modoRango = false) }
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val sesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
        val estado = _uiState.value
        val (desde, hasta) = if (estado.modoRango && estado.fechaDesde != null && estado.fechaHasta != null) {
            estado.fechaDesde to estado.fechaHasta
        } else {
            DateTimeUtils.inicioDeMesEpochMillis(estado.mesVisible) to DateTimeUtils.finDeMesEpochMillis(estado.mesVisible)
        }
        obtenerBalanceMesUseCase(sesionActual.espacioId, desde, hasta).collect { movimientos ->
            _uiState.update {
                it.copy(cargando = false, movimientos = movimientos.map { movimiento -> movimiento.aUi() })
            }
        }
    }
}
