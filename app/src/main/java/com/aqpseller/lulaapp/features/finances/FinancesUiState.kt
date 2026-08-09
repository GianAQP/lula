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
    val gastosHoy: List<MovimientoUi> = emptyList(),
    val movimientosMes: List<MovimientoUi> = emptyList(),
) {
    val balanceMes: Double get() = ingresosMes - gastosMes
    val totalGastosHoy: Double get() = gastosHoy.sumOf { it.monto }
    val hayAlgoRegistrado: Boolean get() = movimientosMes.isNotEmpty()
}
