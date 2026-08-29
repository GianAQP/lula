package com.aqpseller.lulaapp.domain.repository

import kotlinx.coroutines.flow.Flow

/** Un "se quitó a alguien de un Espacio" ya resuelto — quién lo hizo, a quién, y cómo se llamaba
 * esa persona en ese momento (denormalizado desde el registro de auditoría, para no depender de
 * que siga siendo miembro para poder mostrar su nombre). */
data class EventoEliminacionMiembro(
    val timestamp: Long,
    val actorUsuarioId: String,
    val objetivoUsuarioId: String,
    val objetivoNombre: String?,
)

/** Lectura del historial de auditoría (`HistorialCambios`) — escrito desde el MVP por
 * `AuditLogger` en cada repositorio, nunca leído hasta ahora. Ver `Plan/08-decisiones-tecnicas.md`. */
interface HistorialCambiosRepository {
    /** Solo los "quitar de un Espacio" (admin quita a alguien, o alguien sale solo) — quién
     * fue quitado y quién lo hizo, más reciente primero. */
    fun observarEliminacionesDeMiembros(espacioId: String): Flow<List<EventoEliminacionMiembro>>
}
