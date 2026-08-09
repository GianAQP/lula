package com.aqpseller.lulaapp.features.tasks

import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea

data class TaskDetailUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val fechaLimite: Long? = null,
    val importante: Boolean = false,
    val urgente: Boolean = false,
    val estado: EstadoActividad = EstadoActividad.SIN_CONFIRMAR,
    val fechaCompletado: Long? = null,
    val recurrencia: RecurrenciaTarea = RecurrenciaTarea.SIN_REPETIR,
    val nombreActividadVinculada: String? = null,
    val eliminada: Boolean = false,
)
