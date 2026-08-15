package com.aqpseller.lulaapp.features.finances

import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero

data class MovimientoUi(
    val id: String,
    val tipo: TipoMovimientoFinanciero,
    val categoria: String,
    val monto: Double,
    val descripcion: String?,
    val fecha: Long,
)

data class FinancesUiState(
    val cargando: Boolean = true,
    val ingresosMes: Double = 0.0,
    val gastosMes: Double = 0.0,
    val ahorradoMes: Double = 0.0,
    /** Antes solo traía los EGRESOS de hoy ("Gastos de hoy") — un ingreso registrado hoy no
     * aparecía en ningún lado de esta pantalla, solo se reflejaba (sin resaltar) en el total
     * "Este mes" de arriba. A pedido del usuario. Ver `Plan/08-decisiones-tecnicas.md`. */
    val movimientosHoy: List<MovimientoUi> = emptyList(),
    val movimientosMes: List<MovimientoUi> = emptyList(),
) {
    val balanceMes: Double get() = ingresosMes - gastosMes
    val netoHoy: Double get() = movimientosHoy.sumOf { if (it.tipo == TipoMovimientoFinanciero.EGRESO) -it.monto else it.monto }
    val hayAlgoRegistrado: Boolean get() = movimientosMes.isNotEmpty()
}
