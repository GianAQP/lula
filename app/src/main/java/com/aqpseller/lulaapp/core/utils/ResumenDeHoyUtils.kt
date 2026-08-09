package com.aqpseller.lulaapp.core.utils

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.TipoActividad

/**
 * Qué actividades cuentan para el progreso "de hoy" (barra de Hoy, "Cerrar mi día", "Ya hechos
 * hoy") — un solo criterio compartido para que las pantallas nunca vuelvan a mostrar números
 * distintos (el bug reportado: "9 de 14" en Cerrar día contaba Medicamentos que ahí nunca se
 * pueden confirmar, ver `Plan/08-decisiones-tecnicas.md`). Medicamento queda afuera a
 * propósito — se cuenta aparte por toma, no por Actividad completa (`MedicamentoDeHoy.tomas`);
 * Rutina y Fecha importante tampoco son "completables" de un solo toque.
 *
 * Una Tarea ya completada solo sigue contando como "de hoy" si se completó **hoy** — sin esto,
 * una tarea sin fecha límite confirmada ayer (o antes) se quedaba apareciendo como "hecha hoy"
 * para siempre, porque `esTareaPendienteDeHoyOVencida` la consideraba "de hoy" sin importar
 * cuándo se creó (bug reportado, ver `08-decisiones-tecnicas.md`).
 */
fun actividadCuentaParaHoy(actividad: Actividad, estadoSesionCitaHoy: Map<String, EstadoActividad> = emptyMap()): Boolean = when (actividad.tipo) {
    TipoActividad.HABITO -> true
    TipoActividad.TAREA -> if (actividad.estado == EstadoActividad.CONFIRMADO) {
        actividad.fechaCompletado?.let { DateTimeUtils.epochMillisToLocalDate(it) == DateTimeUtils.hoy() } ?: false
    } else {
        esTareaPendienteDeHoyOVencida(actividad.detalle)
    }
    TipoActividad.CITA -> esCitaDeHoy(actividad.detalle, tieneSesionHoy = actividad.id in estadoSesionCitaHoy)
    else -> false
}

/** Estado a usar para contar una actividad "de hoy" — para una Cita de curso, `Actividad.estado`
 * nunca se toca (cada sesión tiene el suyo), así que hay que mirar la sesión de hoy en su lugar. */
fun estadoDeHoy(actividad: Actividad, estadoSesionCitaHoy: Map<String, EstadoActividad> = emptyMap()): EstadoActividad {
    val esCursoDeCita = actividad.tipo == TipoActividad.CITA && (actividad.detalle as? ActividadDetalle.Cita)?.esCurso == true
    return if (esCursoDeCita) estadoSesionCitaHoy[actividad.id] ?: EstadoActividad.SIN_CONFIRMAR else actividad.estado
}

/** Tareas sin fecha límite o con fecha límite hoy/vencida — solo tiene sentido para tareas
 * todavía pendientes; una ya completada se rige por `actividadCuentaParaHoy`. */
fun esTareaPendienteDeHoyOVencida(detalle: ActividadDetalle?): Boolean {
    val tarea = detalle as? ActividadDetalle.Tarea ?: return false
    val fechaLimite = tarea.fechaLimite ?: return true
    return fechaLimite <= DateTimeUtils.finDeHoyEpochMillis()
}

private fun esCitaDeHoy(detalle: ActividadDetalle?, tieneSesionHoy: Boolean): Boolean {
    val cita = detalle as? ActividadDetalle.Cita ?: return false
    return if (cita.esCurso) tieneSesionHoy else DateTimeUtils.epochMillisToLocalDate(cita.fechaHora) == DateTimeUtils.hoy()
}
