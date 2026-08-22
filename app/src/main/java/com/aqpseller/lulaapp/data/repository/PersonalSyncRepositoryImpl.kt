package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.repository.RegistroHabitoRemoto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_ACTIVIDADES = "actividadesPersonales"
private const val SUBCOLECCION_REGISTROS = "registrosHabito"

/**
 * `usuarios/{miFirebaseUid}/actividadesPersonales/{actividadId}` (Hábitos y Tareas, discriminados
 * por campo `tipo`) y `usuarios/{miFirebaseUid}/registrosHabito/{actividadId}_{fecha}`. Solo el
 * dueño (mismo uid) puede leer/escribir — ver `firestore.rules`. No hay `escuchar*` (a
 * diferencia de `EspacioSyncRepository`): lo Personal se restaura una sola vez, no en vivo.
 */
class PersonalSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : PersonalSyncRepository {

    private fun coleccionActividades() =
        firebaseAuth.currentUser?.uid?.let {
            firestore.collection(COLECCION_USUARIOS).document(it).collection(SUBCOLECCION_ACTIVIDADES)
        }

    private fun coleccionRegistros() =
        firebaseAuth.currentUser?.uid?.let {
            firestore.collection(COLECCION_USUARIOS).document(it).collection(SUBCOLECCION_REGISTROS)
        }

    override suspend fun subirHabito(actividad: Actividad, detalle: ActividadDetalle.Habito) {
        val coleccion = coleccionActividades() ?: return
        val datos = mapOf(
            "tipo" to actividad.tipo.name,
            "nombre" to actividad.nombre,
            "propietario" to actividad.propietario,
            "estado" to actividad.estado.name,
            "privacidad" to actividad.privacidad.name,
            "esPremiumFeature" to actividad.esPremiumFeature,
            "areaDeVidaId" to actividad.areaDeVidaId,
            "momentoDelDia" to actividad.momentoDelDia?.name,
            "fechaCreacion" to actividad.fechaCreacion,
            "activa" to actividad.activa,
            "detalleFrecuencia" to detalle.frecuencia.name,
            "detalleDiasEspecificos" to detalle.diasEspecificos,
            "detalleDuracionInicialMin" to detalle.duracionInicialMin,
            "detalleDuracionObjetivoMin" to detalle.duracionObjetivoMin,
            "detalleIncrementoMin" to detalle.incrementoMin,
            "detalleFrecuenciaRevisionDias" to detalle.frecuenciaRevisionDias,
            "detalleHoraRecordatorio" to detalle.horaRecordatorio,
            "detalleNivelRecordatorio" to detalle.nivelRecordatorio.name,
            "detalleDuracionActualMin" to detalle.duracionActualMin,
            "detalleProximaRevisionEpochDay" to detalle.proximaRevisionEpochDay,
        )
        coleccion.document(actividad.id).set(datos).await()
    }

    override suspend fun subirRegistroHabito(actividadId: String, fecha: Long, estado: EstadoActividad) {
        val coleccion = coleccionRegistros() ?: return
        val datos = mapOf("actividadId" to actividadId, "fecha" to fecha, "estado" to estado.name)
        coleccion.document("${actividadId}_$fecha").set(datos).await()
    }

    override suspend fun subirTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea) {
        val coleccion = coleccionActividades() ?: return
        val datos = mapOf(
            "tipo" to actividad.tipo.name,
            "nombre" to actividad.nombre,
            "propietario" to actividad.propietario,
            "estado" to actividad.estado.name,
            "privacidad" to actividad.privacidad.name,
            "esPremiumFeature" to actividad.esPremiumFeature,
            "areaDeVidaId" to actividad.areaDeVidaId,
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
        coleccion.document(actividad.id).set(datos).await()
    }

    /** El respaldo no guarda `espacioId` (siempre es el Espacio Personal de quien restaura, un
     * id distinto por instalación) — queda vacío acá a propósito; quien llama (el caso de uso
     * de restauración) DEBE sobreescribirlo con `obtenerEspacioPersonal(usuarioId)` antes de
     * guardar localmente. */
    private fun actividadBaseDesde(doc: DocumentSnapshot, tipo: TipoActividad): Actividad = Actividad(
        id = doc.id,
        tipo = tipo,
        espacioId = "",
        nombre = doc.getString("nombre") ?: "",
        propietario = doc.getString("propietario") ?: "",
        responsables = emptyList(),
        puedeVer = emptyList(),
        puedeRecordar = emptyList(),
        estado = runCatching { EstadoActividad.valueOf(doc.getString("estado") ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR),
        privacidad = runCatching { Privacidad.valueOf(doc.getString("privacidad") ?: "") }.getOrDefault(Privacidad.SOLO_YO),
        syncStatus = SyncStatus.SINCRONIZADO,
        esPremiumFeature = doc.getBoolean("esPremiumFeature") ?: false,
        areaDeVidaId = doc.getString("areaDeVidaId"),
        momentoDelDia = doc.getString("momentoDelDia")?.let { runCatching { MomentoDelDia.valueOf(it) }.getOrNull() },
        fechaCreacion = doc.getLong("fechaCreacion") ?: 0L,
        activa = doc.getBoolean("activa") ?: true,
        detalle = null,
        fechaCompletado = doc.getLong("fechaCompletado"),
    )

    override suspend fun restaurarHabitos(): List<Pair<Actividad, ActividadDetalle.Habito>> {
        val coleccion = coleccionActividades() ?: return emptyList()
        val snapshot = coleccion.whereEqualTo("tipo", TipoActividad.HABITO.name).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val actividad = actividadBaseDesde(doc, TipoActividad.HABITO)
            val detalle = ActividadDetalle.Habito(
                momentoDelDia = actividad.momentoDelDia ?: MomentoDelDia.MANANA,
                frecuencia = runCatching { FrecuenciaHabito.valueOf(doc.getString("detalleFrecuencia") ?: "") }
                    .getOrDefault(FrecuenciaHabito.DIARIA),
                diasEspecificos = (doc.get("detalleDiasEspecificos") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                duracionInicialMin = doc.getLong("detalleDuracionInicialMin")?.toInt(),
                duracionObjetivoMin = doc.getLong("detalleDuracionObjetivoMin")?.toInt(),
                incrementoMin = doc.getLong("detalleIncrementoMin")?.toInt(),
                frecuenciaRevisionDias = doc.getLong("detalleFrecuenciaRevisionDias")?.toInt(),
                horaRecordatorio = doc.getString("detalleHoraRecordatorio"),
                nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(doc.getString("detalleNivelRecordatorio") ?: "") }
                    .getOrDefault(NivelRecordatorio.SONIDO),
                duracionActualMin = doc.getLong("detalleDuracionActualMin")?.toInt(),
                proximaRevisionEpochDay = doc.getLong("detalleProximaRevisionEpochDay"),
            )
            actividad to detalle
        }
    }

    override suspend fun restaurarRegistrosHabito(): List<RegistroHabitoRemoto> {
        val coleccion = coleccionRegistros() ?: return emptyList()
        val snapshot = coleccion.get().await()
        return snapshot.documents.mapNotNull { doc ->
            RegistroHabitoRemoto(
                actividadId = doc.getString("actividadId") ?: return@mapNotNull null,
                fecha = doc.getLong("fecha") ?: return@mapNotNull null,
                estado = runCatching { EstadoActividad.valueOf(doc.getString("estado") ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR),
            )
        }
    }

    override suspend fun restaurarTareas(): List<Pair<Actividad, ActividadDetalle.Tarea>> {
        val coleccion = coleccionActividades() ?: return emptyList()
        val snapshot = coleccion.whereEqualTo("tipo", TipoActividad.TAREA.name).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val actividad = actividadBaseDesde(doc, TipoActividad.TAREA)
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
    }
}
