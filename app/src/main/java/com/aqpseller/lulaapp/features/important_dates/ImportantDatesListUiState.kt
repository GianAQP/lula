package com.aqpseller.lulaapp.features.important_dates

data class FechaImportanteListItemUi(
    val id: String,
    val nombre: String,
    val fechaTexto: String,
    val recurrenciaTexto: String,
)

data class ImportantDatesListUiState(
    val cargando: Boolean = true,
    val fechas: List<FechaImportanteListItemUi> = emptyList(),
)
