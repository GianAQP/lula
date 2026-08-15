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
    /** Marca de "esto se eliminó" reconstruida desde `historial_cambios` — a pedido del usuario,
     * para que borrar algo deje rastro en Calendario en vez de desaparecer sin dejar huella. Se
     * muestra el día en que se eliminó (no en su fecha original), es de solo lectura: no lleva a
     * ningún detalle ni se puede marcar. Ver `Plan/08-decisiones-tecnicas.md`. */
    val eliminado: Boolean = false,
)
