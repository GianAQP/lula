package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.FrecuenciaRetoFamiliar
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.repository.CodigoInvitacionEspacio
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.RegistroRetoRemoto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_ESPACIOS = "espacios"
private const val COLECCION_CODIGOS_INVITACION = "codigosInvitacionEspacio"

/** El código dura poco a propósito — un QR "para siempre" se podría guardar y usar después sin
 * que la persona que invitó se entere. Ver `Plan/08-decisiones-tecnicas.md`. */
private const val DURACION_CODIGO_MS = 60_000L

/**
 * `espacios/{id}` (root), `espacios/{id}/miembros/{miFirebaseUid}`,
 * `espacios/{id}/actividades/{actividadId}` (solo tipo TAREA esta ronda),
 * `espacios/{id}/retos/{retoId}` y `espacios/{id}/retos/{retoId}/registros/{usuarioId}_{fecha}`.
 * `miembros` se guarda por `firebaseUid` (no por el `usuarioId` local) porque las reglas de
 * seguridad solo pueden verificar contra `request.auth.uid` — ver `firestore.rules` y la misma
 * nota en `CompartirSyncRepositoryImpl`.
 */
class EspacioSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : EspacioSyncRepository {

    override suspend fun subirEspacio(espacio: Espacio) {
        if (firebaseAuth.currentUser == null) return
        val datos = mapOf(
            "nombre" to espacio.nombre,
            "tipo" to espacio.tipo.name,
            "creadoPor" to espacio.creadoPor,
            "fechaCreacion" to espacio.fechaCreacion,
        )
        firestore.collection(COLECCION_ESPACIOS).document(espacio.id).set(datos).await()
    }

    override suspend fun subirMiembro(espacioId: String, miembro: EspacioMiembro) {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return
        val datos = mapOf("usuarioIdLocal" to miembro.usuarioId, "rol" to miembro.rol.name, "nombre" to miembro.nombre)
        firestore.collection(COLECCION_ESPACIOS).document(espacioId)
            .collection("miembros").document(miFirebaseUid).set(datos).await()
    }

    override suspend fun subirPunteroMiEspacio(espacioId: String) {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("usuarios").document(miFirebaseUid)
            .collection("misEspacios").document(espacioId).set(mapOf("espacioId" to espacioId)).await()
    }

    override suspend fun descubrirMisEspacios(): List<Pair<Espacio, EspacioMiembro>> {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return emptyList()
        val punteros = firestore.collection("usuarios").document(miFirebaseUid)
            .collection("misEspacios").get().await()
        return punteros.documents.mapNotNull { puntero ->
            val espacioId = puntero.id
            val espacioDoc = firestore.collection(COLECCION_ESPACIOS).document(espacioId).get().await()
            if (!espacioDoc.exists()) return@mapNotNull null
            val miembroDoc = firestore.collection(COLECCION_ESPACIOS).document(espacioId)
                .collection("miembros").document(miFirebaseUid).get().await()
            if (!miembroDoc.exists()) return@mapNotNull null
            val espacio = Espacio(
                id = espacioId,
                tipo = runCatching { TipoEspacio.valueOf(espacioDoc.getString("tipo") ?: "") }.getOrDefault(TipoEspacio.FAMILIA),
                nombre = espacioDoc.getString("nombre") ?: "",
                creadoPor = espacioDoc.getString("creadoPor") ?: "",
                fechaCreacion = espacioDoc.getLong("fechaCreacion") ?: 0L,
            )
            val usuarioIdLocal = miembroDoc.getString("usuarioIdLocal") ?: return@mapNotNull null
            val rol = runCatching { RolEnEspacio.valueOf(miembroDoc.getString("rol") ?: "") }.getOrDefault(RolEnEspacio.MIEMBRO)
            espacio to EspacioMiembro(espacioId = espacioId, usuarioId = usuarioIdLocal, rol = rol)
        }
    }

    override suspend fun generarCodigoInvitacion(espacioId: String, nombreEspacio: String, deNombre: String): CodigoInvitacionEspacio {
        val miFirebaseUid = checkNotNull(firebaseAuth.currentUser?.uid) { "Necesita cuenta vinculada" }
        val codigoId = IdGenerator.newId()
        val expiraEn = System.currentTimeMillis() + DURACION_CODIGO_MS
        val datos = mapOf(
            "espacioId" to espacioId,
            "nombreEspacio" to nombreEspacio,
            "deFirebaseUid" to miFirebaseUid,
            "deNombre" to deNombre,
            "expiraEn" to expiraEn,
            "reclamadoPor" to null,
        )
        firestore.collection(COLECCION_CODIGOS_INVITACION).document(codigoId).set(datos).await()
        return CodigoInvitacionEspacio(codigoId, espacioId, nombreEspacio, miFirebaseUid, deNombre, expiraEn)
    }

    override suspend fun reclamarCodigoInvitacion(codigoId: String): CodigoInvitacionEspacio? {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return null
        val referencia = firestore.collection(COLECCION_CODIGOS_INVITACION).document(codigoId)
        return runCatching {
            firestore.runTransaction { transaccion ->
                val doc = transaccion.get(referencia)
                if (!doc.exists()) return@runTransaction null
                val expiraEn = doc.getLong("expiraEn") ?: return@runTransaction null
                val yaReclamado = doc.getString("reclamadoPor")
                if (yaReclamado != null || expiraEn < System.currentTimeMillis()) return@runTransaction null
                transaccion.update(referencia, "reclamadoPor", miFirebaseUid)
                CodigoInvitacionEspacio(
                    codigoId = codigoId,
                    espacioId = doc.getString("espacioId") ?: return@runTransaction null,
                    nombreEspacio = doc.getString("nombreEspacio") ?: "",
                    deFirebaseUid = doc.getString("deFirebaseUid") ?: return@runTransaction null,
                    deNombre = doc.getString("deNombre") ?: "",
                    expiraEn = expiraEn,
                )
            }.await()
        }.getOrNull()
    }

    override suspend fun subirTarea(espacioId: String, actividad: Actividad, detalle: ActividadDetalle.Tarea) {
        if (firebaseAuth.currentUser == null) return
        val datos = mapOf(
            "tipo" to actividad.tipo.name,
            "nombre" to actividad.nombre,
            "propietario" to actividad.propietario,
            "responsables" to actividad.responsables,
            "puedeVer" to actividad.puedeVer,
            "puedeRecordar" to actividad.puedeRecordar,
            "estado" to actividad.estado.name,
            "privacidad" to actividad.privacidad.name,
            "esPremiumFeature" to actividad.esPremiumFeature,
            "areaDeVidaId" to actividad.areaDeVidaId,
            "momentoDelDia" to actividad.momentoDelDia?.name,
            "fechaCreacion" to actividad.fechaCreacion,
            "activa" to actividad.activa,
            "fechaCompletado" to actividad.fechaCompletado,
            "detalleFechaLimite" to detalle.fechaLimite,
            "detallePrioridad" to detalle.prioridad,
            "detalleImportante" to detalle.importante,
            "detalleUrgente" to detalle.urgente,
            "detalleHoraRecordatorio" to detalle.horaRecordatorio,
            "detalleNivelRecordatorio" to detalle.nivelRecordatorio.name,
            "detalleRecurrencia" to detalle.recurrencia.name,
            "detalleActividadVinculadaId" to detalle.actividadVinculadaId,
        )
        firestore.collection(COLECCION_ESPACIOS).document(espacioId)
            .collection("actividades").document(actividad.id).set(datos).await()
    }

    override suspend fun subirReto(espacioId: String, reto: RetoFamiliar) {
        if (firebaseAuth.currentUser == null) return
        val datos = mapOf(
            "nombre" to reto.nombre,
            "objetivo" to reto.objetivo,
            "frecuencia" to reto.frecuencia.name,
            "participantesIds" to reto.participantesIds,
            "recompensa" to reto.recompensa,
        )
        firestore.collection(COLECCION_ESPACIOS).document(espacioId)
            .collection("retos").document(reto.id).set(datos).await()
    }

    override suspend fun subirRegistroReto(espacioId: String, registro: RegistroRetoRemoto) {
        if (firebaseAuth.currentUser == null) return
        val datos = mapOf(
            "retoId" to registro.retoId,
            "usuarioId" to registro.usuarioId,
            "fecha" to registro.fecha,
            "estado" to registro.estado.name,
        )
        // Colección plana bajo el espacio (no anidada bajo cada reto) — evita necesitar un
        // collectionGroup query con índice compuesto para escuchar todos los registros a la vez.
        firestore.collection(COLECCION_ESPACIOS).document(espacioId)
            .collection("registrosReto").document("${registro.retoId}_${registro.usuarioId}_${registro.fecha}")
            .set(datos).await()
    }

    override fun escucharMiembros(espacioId: String): Flow<List<EspacioMiembro>> = callbackFlow {
        val registro = firestore.collection(COLECCION_ESPACIOS).document(espacioId).collection("miembros")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(
                    snapshot.documents.mapNotNull { doc ->
                        val usuarioId = doc.getString("usuarioIdLocal") ?: return@mapNotNull null
                        val rol = runCatching { RolEnEspacio.valueOf(doc.getString("rol") ?: "") }
                            .getOrDefault(RolEnEspacio.MIEMBRO)
                        EspacioMiembro(espacioId = espacioId, usuarioId = usuarioId, rol = rol, nombre = doc.getString("nombre"))
                    },
                )
            }
        awaitClose { registro.remove() }
    }

    override fun escucharTareas(espacioId: String): Flow<List<Pair<Actividad, ActividadDetalle.Tarea>>> = callbackFlow {
        val registro = firestore.collection(COLECCION_ESPACIOS).document(espacioId).collection("actividades")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tareas = snapshot.documents.mapNotNull { doc ->
                    if (doc.getString("tipo") != TipoActividad.TAREA.name) return@mapNotNull null
                    val actividad = Actividad(
                        id = doc.id,
                        tipo = TipoActividad.TAREA,
                        espacioId = espacioId,
                        nombre = doc.getString("nombre") ?: return@mapNotNull null,
                        propietario = doc.getString("propietario") ?: return@mapNotNull null,
                        responsables = (doc.get("responsables") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        puedeVer = (doc.get("puedeVer") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        puedeRecordar = (doc.get("puedeRecordar") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        estado = runCatching { EstadoActividad.valueOf(doc.getString("estado") ?: "") }
                            .getOrDefault(EstadoActividad.SIN_CONFIRMAR),
                        privacidad = runCatching { Privacidad.valueOf(doc.getString("privacidad") ?: "") }
                            .getOrDefault(Privacidad.FAMILIA),
                        syncStatus = SyncStatus.SINCRONIZADO,
                        esPremiumFeature = doc.getBoolean("esPremiumFeature") ?: false,
                        areaDeVidaId = doc.getString("areaDeVidaId"),
                        momentoDelDia = doc.getString("momentoDelDia")?.let { runCatching { MomentoDelDia.valueOf(it) }.getOrNull() },
                        fechaCreacion = doc.getLong("fechaCreacion") ?: 0L,
                        activa = doc.getBoolean("activa") ?: true,
                        detalle = null,
                        fechaCompletado = doc.getLong("fechaCompletado"),
                    )
                    val detalle = ActividadDetalle.Tarea(
                        fechaLimite = doc.getLong("detalleFechaLimite"),
                        prioridad = doc.getLong("detallePrioridad")?.toInt(),
                        importante = doc.getBoolean("detalleImportante") ?: false,
                        urgente = doc.getBoolean("detalleUrgente") ?: false,
                        horaRecordatorio = doc.getString("detalleHoraRecordatorio"),
                        nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(doc.getString("detalleNivelRecordatorio") ?: "") }
                            .getOrDefault(NivelRecordatorio.SONIDO),
                        recurrencia = runCatching { RecurrenciaTarea.valueOf(doc.getString("detalleRecurrencia") ?: "") }
                            .getOrDefault(RecurrenciaTarea.SIN_REPETIR),
                        actividadVinculadaId = doc.getString("detalleActividadVinculadaId"),
                    )
                    actividad to detalle
                }
                trySend(tareas)
            }
        awaitClose { registro.remove() }
    }

    override fun escucharRetos(espacioId: String): Flow<List<RetoFamiliar>> = callbackFlow {
        val registro = firestore.collection(COLECCION_ESPACIOS).document(espacioId).collection("retos")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(
                    snapshot.documents.mapNotNull { doc ->
                        RetoFamiliar(
                            id = doc.id,
                            espacioId = espacioId,
                            nombre = doc.getString("nombre") ?: return@mapNotNull null,
                            objetivo = doc.getString("objetivo") ?: "",
                            frecuencia = runCatching { FrecuenciaRetoFamiliar.valueOf(doc.getString("frecuencia") ?: "") }
                                .getOrDefault(FrecuenciaRetoFamiliar.DIARIA),
                            participantesIds = (doc.get("participantesIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            recompensa = doc.getString("recompensa"),
                        )
                    },
                )
            }
        awaitClose { registro.remove() }
    }

    override fun escucharRegistrosReto(espacioId: String): Flow<List<RegistroRetoRemoto>> = callbackFlow {
        val registro = firestore.collection(COLECCION_ESPACIOS).document(espacioId).collection("registrosReto")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(
                    snapshot.documents.mapNotNull { doc ->
                        RegistroRetoRemoto(
                            retoId = doc.getString("retoId") ?: return@mapNotNull null,
                            usuarioId = doc.getString("usuarioId") ?: return@mapNotNull null,
                            fecha = doc.getLong("fecha") ?: return@mapNotNull null,
                            estado = runCatching { EstadoActividad.valueOf(doc.getString("estado") ?: "") }
                                .getOrDefault(EstadoActividad.SIN_CONFIRMAR),
                        )
                    },
                )
            }
        awaitClose { registro.remove() }
    }
}
