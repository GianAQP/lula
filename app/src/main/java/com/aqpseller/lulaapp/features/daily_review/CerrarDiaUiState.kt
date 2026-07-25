package com.aqpseller.lulaapp.features.daily_review

data class CerrarDiaUiState(
    val cargando: Boolean = true,
    val actividadesCompletadas: Int = 0,
    val actividadesTotales: Int = 0,
    val cerrado: Boolean = false,
    val rachaFinal: Int = 0,
)
