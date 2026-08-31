package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.DiaHistorialHabito
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.SesionCita
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoAviso
import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import com.aqpseller.lulaapp.domain.repository.ActividadCompartidaRemota
import com.aqpseller.lulaapp.domain.repository.CareCircleContenidoSyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_ACTIVIDADES_COMPARTIDAS = "actividadesCompartidas"

/**
 * `actividadesCompartidas/{solicitudId}` — un documento por `SolicitudCompartir` ya aceptada de
 * tipo ACTIVIDAD. Solo el que comparte escribe (`deFirebaseUid`); el que comparte y a quien le
 * comparten (`paraCorreo`, correo verificado) pueden leer. Ver `firestore.rules` y
 * `Plan/08-decisiones-tecnicas.md`.
 */
class CareCircleContenidoSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : CareCircleContenidoSyncRepository {

    override suspend fun subirActividadCompartida(
        solicitudId: String,
        paraCorreo: String,
        deNombre: String,
        permiso: PermisoCompartir,
        actividad: Actividad,
        detalle: ActividadDetalle?,
        historialHabito: List<DiaHistorialHabito>,
        tomasRecientes: List<TomaMedicamento>,
        sesionesCita: List<SesionCita>,
    ) {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return
        val datos = mutableMapOf<String, Any?>(
            "deFirebaseUid" to miFirebaseUid,
            "deNombre" to deNombre,
            "paraCorreo" to paraCorreo,
            "permiso" to permiso.name,
            "tipo" to actividad.tipo.name,
            "nombre" to actividad.nombre,
            "estado" to actividad.estado.name,
            "privacidad" to actividad.privacidad.name,
            "areaDeVidaId" to actividad.areaDeVidaId,
            "momentoDelDia" to actividad.momentoDelDia?.name,
            "fechaCreacion" to actividad.fechaCreacion,
            "activa" to actividad.activa,
            "fechaCompletado" to actividad.fechaCompletado,
        )
        when (detalle) {
            is ActividadDetalle.Habito -> {
                datos["detalleFrecuencia"] = detalle.frecuencia.name
                datos["detalleNivelRecordatorio"] = detalle.nivelRecordatorio.name
                datos["historialHabito"] = historialHabito.map { mapOf("fecha" to it.fecha, "estado" to it.estado.name) }
            }
            is ActividadDetalle.Tarea -> {
                datos["detalleFechaLimite"] = detalle.fechaLimite
                datos["detalleImportante"] = detalle.importante
                datos["detalleUrgente"] = detalle.urgente
            }
            is ActividadDetalle.Rutina -> {
                datos["detalleActividadesIncluidasIds"] = detalle.actividadesIncluidasIds
            }
            is ActividadDetalle.Medicamento -> {
                datos["detalleNombreMedicamento"] = detalle.nombreMedicamento
                datos["detalleDosis"] = detalle.dosis
                datos["detalleHorariosCalculados"] = detalle.horariosCalculados
                datos["detalleFechaInicio"] = detalle.fechaInicio
                datos["detalleFechaFin"] = detalle.fechaFin
                datos["detalleNivelRecordatorio"] = detalle.nivelRecordatorio.name
                datos["tomasRecientes"] = tomasRecientes.map {
                    mapOf("fecha" to it.fecha, "horario" to it.horario, "estado" to it.estado.name)
                }
            }
            is ActividadDetalle.Cita -> {
                datos["detalleLugar"] = detalle.lugar
                datos["detalleMotivo"] = detalle.motivo
                datos["detalleFechaHora"] = detalle.fechaHora
                datos["detalleEsCurso"] = detalle.esCurso
                datos["sesionesCita"] = sesionesCita.map {
                    mapOf(
                        "numeroSesion" to it.numeroSesion, "fecha" to it.fecha,
                        "fechaOriginal" to it.fechaOriginal, "horario" to it.horario, "estado" to it.estado.name,
                    )
                }
            }
            is ActividadDetalle.FechaImportante -> {
                datos["detalleRecurrencia"] = detalle.recurrencia.name
                datos["detalleFechaBase"] = detalle.fechaBase
                datos["detalleAnticipacion"] = detalle.anticipacion.name
                datos["detalleTipoAviso"] = detalle.tipoAviso.name
            }
            null -> Unit
        }
        firestore.collection(COLECCION_ACTIVIDADES_COMPARTIDAS).document(solicitudId).set(datos).await()
    }

    override suspend fun eliminarActividadCompartida(solicitudId: String) {
        firestore.collection(COLECCION_ACTIVIDADES_COMPARTIDAS).document(solicitudId).delete().await()
    }

    override fun escucharActividadesCompartidasConmigo(miCorreo: String): Flow<List<ActividadCompartidaRemota>> = callbackFlow {
        if (miCorreo.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val registro = firestore.collection(COLECCION_ACTIVIDADES_COMPARTIDAS)
            .whereEqualTo("paraCorreo", miCorreo)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.documents.mapNotNull { doc -> mapearDoc(doc) })
            }
        awaitClose { registro.remove() }
    }

    private fun mapearDoc(doc: DocumentSnapshot): ActividadCompartidaRemota? {
        val tipo = runCatching { TipoActividad.valueOf(doc.getString("tipo") ?: "") }.getOrNull() ?: return null
        val actividad = Actividad(
            id = doc.id,
            tipo = tipo,
            espacioId = "",
            nombre = doc.getString("nombre") ?: "",
            propietario = doc.getString("deNombre") ?: "",
            responsables = emptyList(),
            puedeVer = emptyList(),
            puedeRecordar = emptyList(),
            estado = runCatching { EstadoActividad.valueOf(doc.getString("estado") ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR),
            privacidad = runCatching { Privacidad.valueOf(doc.getString("privacidad") ?: "") }.getOrDefault(Privacidad.SOLO_YO),
            syncStatus = SyncStatus.SINCRONIZADO,
            esPremiumFeature = false,
            areaDeVidaId = doc.getString("areaDeVidaId"),
            momentoDelDia = doc.getString("momentoDelDia")?.let { runCatching { MomentoDelDia.valueOf(it) }.getOrNull() },
            fechaCreacion = doc.getLong("fechaCreacion") ?: 0L,
            activa = doc.getBoolean("activa") ?: true,
            detalle = null,
            fechaCompletado = doc.getLong("fechaCompletado"),
        )
        var historial: List<DiaHistorialHabito> = emptyList()
        var tomas: List<TomaMedicamento> = emptyList()
        var sesiones: List<SesionCita> = emptyList()
        val detalle: ActividadDetalle? = when (tipo) {
            TipoActividad.HABITO -> {
                historial = (doc.get("historialHabito") as? List<*>)?.mapNotNull { raw ->
                    val m = raw as? Map<*, *> ?: return@mapNotNull null
                    val fecha = (m["fecha"] as? Number)?.toLong() ?: return@mapNotNull null
                    val estado = runCatching { EstadoActividad.valueOf(m["estado"] as? String ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR)
                    DiaHistorialHabito(fecha, estado)
                } ?: emptyList()
                ActividadDetalle.Habito(
                    momentoDelDia = actividad.momentoDelDia ?: MomentoDelDia.MANANA,
                    frecuencia = runCatching { FrecuenciaHabito.valueOf(doc.getString("detalleFrecuencia") ?: "") }.getOrDefault(FrecuenciaHabito.DIARIA),
                    nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(doc.getString("detalleNivelRecordatorio") ?: "") }.getOrDefault(NivelRecordatorio.SONIDO),
                )
            }
            TipoActividad.TAREA -> ActividadDetalle.Tarea(
                fechaLimite = doc.getLong("detalleFechaLimite"),
                importante = doc.getBoolean("detalleImportante") ?: false,
                urgente = doc.getBoolean("detalleUrgente") ?: false,
            )
            TipoActividad.RUTINA -> ActividadDetalle.Rutina(
                actividadesIncluidasIds = (doc.get("detalleActividadesIncluidasIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                momentoDelDia = actividad.momentoDelDia ?: MomentoDelDia.MANANA,
            )
            TipoActividad.MEDICAMENTO -> {
                tomas = (doc.get("tomasRecientes") as? List<*>)?.mapNotNull { raw ->
                    val m = raw as? Map<*, *> ?: return@mapNotNull null
                    val fecha = (m["fecha"] as? Number)?.toLong() ?: return@mapNotNull null
                    val horario = m["horario"] as? String ?: return@mapNotNull null
                    val estado = runCatching { EstadoActividad.valueOf(m["estado"] as? String ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR)
                    TomaMedicamento(id = "$fecha:$horario", actividadId = doc.id, fecha = fecha, horario = horario, estado = estado)
                } ?: emptyList()
                ActividadDetalle.Medicamento(
                    nombreMedicamento = doc.getString("detalleNombreMedicamento") ?: "",
                    dosis = doc.getString("detalleDosis") ?: "",
                    modoFrecuencia = ModoFrecuenciaMedicamento.INTERVALO_HORAS,
                    horariosCalculados = (doc.get("detalleHorariosCalculados") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    fechaInicio = doc.getLong("detalleFechaInicio") ?: 0L,
                    fechaFin = doc.getLong("detalleFechaFin"),
                    nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(doc.getString("detalleNivelRecordatorio") ?: "") }.getOrDefault(NivelRecordatorio.SONIDO),
                )
            }
            TipoActividad.CITA -> {
                sesiones = (doc.get("sesionesCita") as? List<*>)?.mapNotNull { raw ->
                    val m = raw as? Map<*, *> ?: return@mapNotNull null
                    SesionCita(
                        id = "${doc.id}:${m["numeroSesion"]}",
                        actividadId = doc.id,
                        numeroSesion = (m["numeroSesion"] as? Number)?.toInt() ?: return@mapNotNull null,
                        fecha = (m["fecha"] as? Number)?.toLong() ?: return@mapNotNull null,
                        fechaOriginal = (m["fechaOriginal"] as? Number)?.toLong() ?: 0L,
                        horario = m["horario"] as? String ?: "",
                        estado = runCatching { EstadoActividad.valueOf(m["estado"] as? String ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR),
                    )
                } ?: emptyList()
                ActividadDetalle.Cita(
                    lugar = doc.getString("detalleLugar"),
                    motivo = doc.getString("detalleMotivo"),
                    fechaHora = doc.getLong("detalleFechaHora") ?: 0L,
                    esCurso = doc.getBoolean("detalleEsCurso") ?: false,
                )
            }
            TipoActividad.FECHA_IMPORTANTE -> ActividadDetalle.FechaImportante(
                recurrencia = runCatching { Recurrencia.valueOf(doc.getString("detalleRecurrencia") ?: "") }.getOrDefault(Recurrencia.ANUAL),
                fechaBase = doc.getLong("detalleFechaBase") ?: 0L,
                horaNotificacion = "09:00",
                anticipacion = runCatching { AnticipacionRecordatorio.valueOf(doc.getString("detalleAnticipacion") ?: "") }.getOrDefault(AnticipacionRecordatorio.MISMO_DIA),
                tipoAviso = runCatching { TipoAviso.valueOf(doc.getString("detalleTipoAviso") ?: "") }.getOrDefault(TipoAviso.MENSAJE_SILENCIOSO),
            )
        }
        val deFirebaseUid = doc.getString("deFirebaseUid") ?: return null
        return ActividadCompartidaRemota(
            solicitudId = doc.id,
            deNombre = doc.getString("deNombre") ?: "Alguien",
            deFirebaseUid = deFirebaseUid,
            permiso = runCatching { PermisoCompartir.valueOf(doc.getString("permiso") ?: "") }.getOrDefault(PermisoCompartir.PUEDE_VER),
            actividad = actividad,
            detalle = detalle,
            historialHabito = historial,
            tomasRecientes = tomas,
            sesionesCita = sesiones,
        )
    }
}
