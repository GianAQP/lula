package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import kotlinx.coroutines.flow.Flow

/** Un registro de "cumplido hoy" de Reto familiar tal como llega de Firestore. */
data class RegistroRetoRemoto(val retoId: String, val usuarioId: String, val fecha: Long, val estado: EstadoActividad)

/** Código de invitación a un Espacio Familia con tiempo de vida corto — ver
 * `EspacioSyncRepository.generarCodigoInvitacion`/`reclamarCodigoInvitacion`. */
data class CodigoInvitacionEspacio(
    val codigoId: String,
    val espacioId: String,
    val nombreEspacio: String,
    val deFirebaseUid: String,
    val deNombre: String,
    val expiraEn: Long,
)

/**
 * Espejo en Firestore del *contenido* de un Espacio Familia (paso 5 de
 * `Plan/12-firebase-auth-y-sync.md`) — Tareas del hogar y Retos familiares, que es lo que
 * `FamiliaScreen` ya ofrece hoy. Hábitos/Medicamentos/Citas quedan fuera de esta ronda (son más
 * de uso personal, ver Círculo de cuidado). Todo método de subida es best-effort: quien llama
 * debe envolverlo en `runCatching` y nunca bloquear la acción local si Firestore falla.
 */
interface EspacioSyncRepository {
    suspend fun subirEspacio(espacio: Espacio)

    /** Siempre escribe MI PROPIA membresía (la sesión autenticada actual) — nunca en nombre de
     * otro, las reglas de seguridad no lo permitirían. */
    suspend fun subirMiembro(espacioId: String, miembro: EspacioMiembro)

    /** Un admin cambia el rol de OTRO miembro (hacer admin / quitar admin) — a diferencia de
     * [subirMiembro], sí escribe en el documento de otra persona; la regla de seguridad solo
     * permite tocar el campo `rol` en ese caso. */
    suspend fun actualizarRolMiembro(espacioId: String, miembroFirebaseUid: String, nuevoRol: RolEnEspacio)

    /** Borra la membresía de [miembroFirebaseUid] — sirve tanto para "salir del espacio" (borro
     * la mía) como para que un admin quite a otra persona (las reglas de seguridad exigen ser
     * esa persona O admin del espacio, ver `firestore.rules`). */
    suspend fun eliminarMiembro(espacioId: String, miembroFirebaseUid: String)

    /** Borra el Espacio completo (todas sus subcolecciones + el puntero `misEspacios` propio) —
     * a diferencia de [eliminarMiembro], esto es "eliminar para siempre", no "salir". */
    suspend fun eliminarEspacioCompleto(espacioId: String)

    suspend fun subirTarea(espacioId: String, actividad: Actividad, detalle: ActividadDetalle.Tarea)
    suspend fun subirReto(espacioId: String, reto: RetoFamiliar)
    suspend fun subirRegistroReto(espacioId: String, registro: RegistroRetoRemoto)

    /** Deja un puntero liviano en mi propio perfil (`usuarios/{miFirebaseUid}/misEspacios/{id}`)
     * — así, en un celular nuevo, puedo saber en qué Espacios Familia soy miembro sin tener que
     * buscar en toda la base (una `collectionGroup` query necesitaría un índice compuesto que no
     * se puede crear sin Firebase CLI en este entorno). Ver `Plan/12-firebase-auth-y-sync.md`. */
    suspend fun subirPunteroMiEspacio(espacioId: String)

    /** Todos los Espacios Familia en los que aparezco como miembro — para restaurarlos en un
     * celular nuevo al vincular la cuenta. Pensado para correr una sola vez, no un listener. */
    suspend fun descubrirMisEspacios(): List<Pair<Espacio, EspacioMiembro>>

    /** Crea un código de invitación de un solo uso, válido por poco tiempo (`expiraEn`) — pensado
     * para mostrarse como QR y regenerarse solo antes de vencer (ver `FamiliaViewModel`). Evita
     * el riesgo de un QR "para siempre" que cualquiera podría guardar y usar después. */
    suspend fun generarCodigoInvitacion(espacioId: String, nombreEspacio: String, deNombre: String): CodigoInvitacionEspacio

    /** Intenta reclamar un código — null si no existe, ya venció, o ya lo reclamó otra persona.
     * Si lo devuelve, YA quedó marcado como reclamado (nadie más puede volver a usarlo). */
    suspend fun reclamarCodigoInvitacion(codigoId: String): CodigoInvitacionEspacio?

    fun escucharMiembros(espacioId: String): Flow<List<EspacioMiembro>>
    fun escucharTareas(espacioId: String): Flow<List<Pair<Actividad, ActividadDetalle.Tarea>>>
    fun escucharRetos(espacioId: String): Flow<List<RetoFamiliar>>
    fun escucharRegistrosReto(espacioId: String): Flow<List<RegistroRetoRemoto>>
}
