package com.aqpseller.lulaapp.features.history

data class RegistroDiarioUi(
    val id: String,
    val fechaTexto: String,
    val actividadesCompletadas: Int,
    val actividadesTotales: Int,
    val puntuacion: Int,
    val queLogre: String?,
    val queCosto: String?,
    val queAjusto: String?,
)

data class HistoryUiState(
    val cargando: Boolean = true,
    val registros: List<RegistroDiarioUi> = emptyList(),
    val constancia: Int = 0,
)
