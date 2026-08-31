package com.aqpseller.lulaapp.features.finances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.usecase.finanzas.BuscarMovimientosUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
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
@OptIn(FlowPreview::class)
@HiltViewModel
class FinancesHistoryViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
    private val buscarMovimientosUseCase: BuscarMovimientosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancesHistoryUiState(mesVisible = DateTimeUtils.hoy()))
    val uiState: StateFlow<FinancesHistoryUiState> = _uiState.asStateFlow()

    private val consulta = MutableStateFlow("")
    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            cargar()
        }
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            // Busca en TODO el historial, no solo el período visible — "¿cuándo gasté eso?".
            // `debounce` + `flatMapLatest`: no dispara una consulta por cada tecla, y cancela la
            // anterior si llega una más nueva. Ver `Plan/08-decisiones-tecnicas.md`.
            consulta
                .debounce(250)
                .filter { it.isNotBlank() }
                .flatMapLatest { texto -> buscarMovimientosUseCase(sesionActual.espacioId, texto.trim()) }
                .collect { movimientos ->
                    _uiState.update { it.copy(resultadosBusqueda = movimientos.map { m -> m.aUi() }) }
                }
        }
    }

    fun buscar(texto: String) {
        _uiState.update { it.copy(consulta = texto) }
        consulta.value = texto
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

    /** Filas separadas por tabulador — pegan directo como columnas en Excel/Sheets. Ver
     * `Plan/08-decisiones-tecnicas.md`. */
    fun textoParaCopiar(): String {
        val estado = _uiState.value
        val encabezado = "Fecha\tTipo\tCategoría\tMonto\tDescripción"
        val filas = estado.movimientosVisibles.map { m ->
            val tipo = if (m.tipo == TipoMovimientoFinanciero.EGRESO) "Gasto" else "Ingreso"
            val fecha = DateTimeUtils.formatearFechaCorta(DateTimeUtils.epochMillisToLocalDate(m.fecha))
            "$fecha\t$tipo\t${m.categoria}\t${"%.2f".format(m.monto)}\t${m.descripcion.orEmpty()}"
        }
        return (listOf(encabezado) + filas).joinToString("\n")
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
