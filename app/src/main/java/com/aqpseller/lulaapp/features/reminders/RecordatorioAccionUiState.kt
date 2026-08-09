package com.aqpseller.lulaapp.features.reminders

data class RecordatorioAccionUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val esHabito: Boolean = true,
    val listo: Boolean = false,
)
