package com.aqpseller.lulaapp.features.habits

import com.aqpseller.lulaapp.domain.model.MomentoDelDia

data class HabitDetailUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val momentoDelDia: MomentoDelDia? = null,
    val racha: Int = 0,
    val diasHistorial30: List<Boolean> = emptyList(),
    val activa: Boolean = true,
    val eliminado: Boolean = false,
)
