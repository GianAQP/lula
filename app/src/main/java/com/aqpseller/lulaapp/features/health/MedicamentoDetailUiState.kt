package com.aqpseller.lulaapp.features.health

import com.aqpseller.lulaapp.domain.model.EstadoActividad

data class HistorialTomaUi(
    val fechaTexto: String,
    val horario: String,
    val estado: EstadoActividad,
)

data class MedicamentoDetailUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val dosis: String = "",
    val activa: Boolean = true,
    val historial: List<HistorialTomaUi> = emptyList(),
    val nombresTareasVinculadas: List<String> = emptyList(),
    val eliminado: Boolean = false,
)
