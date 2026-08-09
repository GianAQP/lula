package com.aqpseller.lulaapp.features.finances

import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import kotlinx.datetime.LocalDate

data class FinancesHistoryUiState(
    val cargando: Boolean = true,
    val mesVisible: LocalDate,
    val movimientos: List<MovimientoUi> = emptyList(),
    /** true = viendo un rango de fechas elegido a mano (`fechaDesde`/`fechaHasta`), en vez de
     * `mesVisible` — ver `Plan/08-decisiones-tecnicas.md`. */
    val modoRango: Boolean = false,
    val fechaDesde: Long? = null,
    val fechaHasta: Long? = null,
) {
    val totalIngresos: Double get() = movimientos.filter { it.tipo == TipoMovimientoFinanciero.INGRESO }.sumOf { it.monto }
    val totalEgresos: Double get() = movimientos.filter { it.tipo == TipoMovimientoFinanciero.EGRESO }.sumOf { it.monto }
    val balance: Double get() = totalIngresos - totalEgresos
}
