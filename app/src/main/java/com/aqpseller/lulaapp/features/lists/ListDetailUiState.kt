package com.aqpseller.lulaapp.features.lists

import com.aqpseller.lulaapp.domain.model.ListaItem

data class ListDetailUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val items: List<ListaItem> = emptyList(),
    val eliminada: Boolean = false,
)
