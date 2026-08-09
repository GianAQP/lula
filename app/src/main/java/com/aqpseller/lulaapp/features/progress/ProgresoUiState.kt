package com.aqpseller.lulaapp.features.progress

data class ProgresoUiState(
    val cargando: Boolean = true,
    val cumplimientoSemana: Int = 0,
    val rachaMaximaSemana: Int = 0,
    val constancia30Dias: Int = 0,
    val puntosSemana: Int = 0,
)
