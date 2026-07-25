package com.aqpseller.lulaapp.domain.model

data class MovimientoFinanciero(
    val id: String,
    val espacioId: String,
    val tipo: TipoMovimientoFinanciero,
    val monto: Double,
    val categoria: String,
    val descripcion: String?,
    val fecha: Long,
    val privacidad: Privacidad,
)
