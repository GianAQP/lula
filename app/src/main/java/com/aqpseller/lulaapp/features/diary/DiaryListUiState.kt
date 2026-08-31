package com.aqpseller.lulaapp.features.diary

data class DiaryEntryItemUi(
    val id: String,
    val extracto: String,
    val fechaTexto: String,
)

data class DiaryListUiState(
    val cargando: Boolean = true,
    val entradas: List<DiaryEntryItemUi> = emptyList(),
    val consulta: String = "",
)
