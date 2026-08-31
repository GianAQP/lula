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
    /** Búsqueda en TODO el historial (no solo el período visible) — para "¿cuándo gasté eso?".
     * Ver `Plan/08-decisiones-tecnicas.md`. */
    val consulta: String = "",
    val resultadosBusqueda: List<MovimientoUi> = emptyList(),
) {
    /** Lo que de verdad se muestra: resultados de búsqueda si hay una consulta activa, si no el
     * período (mes o rango) actual. */
    val movimientosVisibles: List<MovimientoUi> get() = if (consulta.isBlank()) movimientos else resultadosBusqueda

    val totalIngresos: Double get() = movimientosVisibles.filter { it.tipo == TipoMovimientoFinanciero.INGRESO }.sumOf { it.monto }
    val totalEgresos: Double get() = movimientosVisibles.filter { it.tipo == TipoMovimientoFinanciero.EGRESO }.sumOf { it.monto }
    val balance: Double get() = totalIngresos - totalEgresos

    /** Cuánto se gastó/ingresó por categoría en lo visible, de mayor a menor — a pedido del
     * usuario, para ver en qué se gasta más, sin gráficos (la app no usa charts, solo texto/
     * emoji). Ver `Plan/08-decisiones-tecnicas.md`. */
    val egresosPorCategoria: List<Pair<String, Double>> get() = porCategoria(TipoMovimientoFinanciero.EGRESO)
    val ingresosPorCategoria: List<Pair<String, Double>> get() = porCategoria(TipoMovimientoFinanciero.INGRESO)

    private fun porCategoria(tipo: TipoMovimientoFinanciero): List<Pair<String, Double>> =
        movimientosVisibles.filter { it.tipo == tipo }
            .groupBy { it.categoria }
            .map { (categoria, movimientos) -> categoria to movimientos.sumOf { it.monto } }
            .sortedByDescending { it.second }
}
