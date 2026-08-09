package com.aqpseller.lulaapp.domain.model

/** Una fila del Calendario/Agenda — un Hábito, Tarea, Medicamento, Cita o Fecha importante en un día puntual. */
data class ItemAgenda(
    val actividadId: String,
    val tipo: TipoActividad,
    val nombre: String,
    /** Formato "HH:mm", null si no tiene hora puntual (ej. una Tarea sin hora de recordatorio). */
    val horario: String?,
    val momentoDelDia: MomentoDelDia?,
    val estado: EstadoActividad,
    val subtitulo: String?,
    /** Para Medicamento, el horario específico de esa toma (clave compuesta con `actividadId`). */
    val tomaHorario: String? = null,
    /** Para una Cita de curso, el número de esa sesión (clave compuesta con `actividadId`). */
    val sesionNumero: Int? = null,
)
