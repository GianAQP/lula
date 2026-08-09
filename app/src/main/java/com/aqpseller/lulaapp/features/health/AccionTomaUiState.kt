package com.aqpseller.lulaapp.features.health

import com.aqpseller.lulaapp.domain.model.EstadoActividad

data class AccionTomaUiState(
    val cargando: Boolean = true,
    val nombreMedicamento: String = "",
    val instruccion: String = "",
    val estado: EstadoActividad = EstadoActividad.SIN_CONFIRMAR,
    val listo: Boolean = false,
)
