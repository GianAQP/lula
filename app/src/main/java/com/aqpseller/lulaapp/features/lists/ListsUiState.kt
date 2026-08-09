package com.aqpseller.lulaapp.features.lists

import com.aqpseller.lulaapp.domain.model.ListaResumen

data class ListsUiState(
    val cargando: Boolean = true,
    val listas: List<ListaResumen> = emptyList(),
)
