package com.aqpseller.lulaapp.features.routines

import com.aqpseller.lulaapp.domain.model.MomentoDelDia

data class RutinaListItemUi(
    val id: String,
    val nombre: String,
    val momentoDelDia: MomentoDelDia,
    val completadas: Int,
    val total: Int,
)

data class RoutinesListUiState(
    val cargando: Boolean = true,
    val rutinas: List<RutinaListItemUi> = emptyList(),
)
