package com.aqpseller.lulaapp.features.family

data class RetoFamiliarUi(
    val id: String,
    val nombre: String,
    val objetivo: String,
    val recompensa: String?,
    val cumplidosHoy: Int,
    val totalParticipantes: Int,
    val yoCumpliHoy: Boolean,
)

data class RetosFamiliaresUiState(
    val cargando: Boolean = true,
    val retos: List<RetoFamiliarUi> = emptyList(),
)
