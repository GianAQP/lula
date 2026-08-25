package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.DiaHistorialHabito
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SesionCita
import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import kotlinx.coroutines.flow.Flow

/** Una actividad compartida por otra persona, tal como la ve quien la acompaña — no vive en su
 * base local (es de otro dispositivo), así que viaja completa por acá en vez de solo un
 * permiso. Ver `Plan/08-decisiones-tecnicas.md`. */
data class ActividadCompartidaRemota(
    val solicitudId: String,
    val deNombre: String,
    val permiso: PermisoCompartir,
    val actividad: Actividad,
    val detalle: ActividadDetalle?,
    val historialHabito: List<DiaHistorialHabito> = emptyList(),
    val tomasRecientes: List<TomaMedicamento> = emptyList(),
    val sesionesCita: List<SesionCita> = emptyList(),
)

/**
 * Espejo en Firestore del *contenido* de una actividad compartida por Círculo de cuidado — a
 * diferencia de `SolicitudCompartir`/`Conexion` (la "capa social", quién le compartió a quién),
 * esto es lo que realmente se ve: el hábito/tarea/medicamento en sí, con su estado actual. Un
 * documento por `SolicitudCompartir` ya aceptada (`tipo = ACTIVIDAD`) — solo quien la compartió
 * escribe, solo el que compartió y a quien se le compartió (por correo verificado) pueden leer.
 * Ver `Plan/08-decisiones-tecnicas.md`.
 */
interface CareCircleContenidoSyncRepository {
    suspend fun subirActividadCompartida(
        solicitudId: String,
        paraCorreo: String,
        deNombre: String,
        permiso: PermisoCompartir,
        actividad: Actividad,
        detalle: ActividadDetalle?,
        historialHabito: List<DiaHistorialHabito> = emptyList(),
        tomasRecientes: List<TomaMedicamento> = emptyList(),
        sesionesCita: List<SesionCita> = emptyList(),
    )

    suspend fun eliminarActividadCompartida(solicitudId: String)

    /** En vivo mientras se escuche — todo lo que otras personas me comparten a mí, filtrado por
     * mi correo verificado. Vacío si la cuenta no está vinculada. */
    fun escucharActividadesCompartidasConmigo(miCorreo: String): Flow<List<ActividadCompartidaRemota>>
}
