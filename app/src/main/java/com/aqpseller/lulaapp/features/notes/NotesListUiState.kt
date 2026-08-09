package com.aqpseller.lulaapp.features.notes

data class NotaListItemUi(
    val id: String,
    val titulo: String,
    val preview: String,
    val fechaTexto: String,
    val orden: Int,
)

data class NotesListUiState(
    val cargando: Boolean = true,
    val notas: List<NotaListItemUi> = emptyList(),
)
