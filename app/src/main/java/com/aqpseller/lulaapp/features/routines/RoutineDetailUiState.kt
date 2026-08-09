package com.aqpseller.lulaapp.features.routines

import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia

data class RutinaItemUi(
    val id: String,
    val nombre: String,
    val estado: EstadoActividad,
)

data class RoutineDetailUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val momentoDelDia: MomentoDelDia = MomentoDelDia.MANANA,
    val items: List<RutinaItemUi> = emptyList(),
    val eliminada: Boolean = false,
)
