package com.aqpseller.lulaapp.features.privacy

data class PrivacyGateUiState(
    val cargando: Boolean = true,
    val configurada: Boolean = false,
    val desbloqueada: Boolean = false,
    val biometriaDisponible: Boolean = false,
    val pinIncorrecto: Boolean = false,
)
