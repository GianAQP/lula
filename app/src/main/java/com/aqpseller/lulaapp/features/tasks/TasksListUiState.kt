package com.aqpseller.lulaapp.features.tasks

import com.aqpseller.lulaapp.domain.model.EstadoActividad

data class TareaListItemUi(
    val id: String,
    val nombre: String,
    val fechaLimite: Long?,
    val estado: EstadoActividad,
    val importante: Boolean,
    val urgente: Boolean,
)

data class TasksListUiState(
    val cargando: Boolean = true,
    val tareas: List<TareaListItemUi> = emptyList(),
)
