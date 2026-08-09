package com.aqpseller.lulaapp.features.lists

import com.aqpseller.lulaapp.domain.model.ItemListaSnapshot

data class ListaEjecucionItemUi(
    val id: String,
    val fechaTexto: String,
    val marcados: Int,
    val total: Int,
    val items: List<ItemListaSnapshot>,
)

data class ListHistoryUiState(
    val cargando: Boolean = true,
    val nombreLista: String = "",
    val ejecuciones: List<ListaEjecucionItemUi> = emptyList(),
)
