package com.aqpseller.lulaapp.domain.model

/**
 * Historial permanente de avisos (invitaciones, respuestas, "Recordarle") — a diferencia de la
 * notificación del sistema (que se posta y se pierde), esto queda guardado en la app como
 * registro, igual que el historial de notificaciones de cualquier app real. Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
data class Notificacion(
    val id: String,
    val emoji: String,
    val titulo: String,
    val cuerpo: String,
    val fecha: Long,
    val leido: Boolean,
    val solicitudId: String?,
)
