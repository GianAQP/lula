package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.CategoriaMeta
import com.aqpseller.lulaapp.domain.model.Comida
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.ListaConItems
import com.aqpseller.lulaapp.domain.model.ListaItem
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.MomentoRelativoComida
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.model.Notificacion
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import com.aqpseller.lulaapp.domain.model.RecordatorioCita
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.model.SesionCita
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoAviso
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.repository.RegistroHabitoRemoto
import com.aqpseller.lulaapp.domain.repository.TomaMedicamentoRemota
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_USUARIOS = "usuarios"
private const val SUBCOLECCION_ACTIVIDADES = "actividadesPersonales"
private const val SUBCOLECCION_REGISTROS = "registrosHabito"
private const val SUBCOLECCION_TOMAS = "tomasMedicamento"
private const val SUBCOLECCION_SESIONES_CITA = "sesionesCita"
private const val SUBCOLECCION_FINANZAS = "movimientosFinancieros"
private const val SUBCOLECCION_DIARIO = "entradasDiario"
private const val SUBCOLECCION_NOTAS = "notas"
private const val SUBCOLECCION_METAS = "metas"
private const val SUBCOLECCION_LISTAS = "listas"
private const val SUBCOLECCION_PROPOSITO = "proposito"
private const val SUBCOLECCION_REGISTROS_DIARIOS = "registrosDiarios"
private const val SUBCOLECCION_REGISTROS_SEMANALES = "registrosSemanales"
private const val SUBCOLECCION_NOTIFICACIONES = "notificaciones"

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

    private fun coleccion(nombre: String) =
        firebaseAuth.currentUser?.uid?.let { firestore.collection(COLECCION_USUARIOS).document(it).collection(nombre) }

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

    override suspend fun subirRutina(actividad: Actividad, detalle: ActividadDetalle.Rutina) {
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
            "detalleActividadesIncluidasIds" to detalle.actividadesIncluidasIds,
        )
        coleccion.document(actividad.id).set(datos).await()
    }

    override suspend fun restaurarRutinas(): List<Pair<Actividad, ActividadDetalle.Rutina>> {
        val coleccion = coleccionActividades() ?: return emptyList()
        val snapshot = coleccion.whereEqualTo("tipo", TipoActividad.RUTINA.name).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val actividad = actividadBaseDesde(doc, TipoActividad.RUTINA)
            val detalle = ActividadDetalle.Rutina(
                actividadesIncluidasIds = (doc.get("detalleActividadesIncluidasIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                momentoDelDia = actividad.momentoDelDia ?: MomentoDelDia.MANANA,
            )
            actividad to detalle
        }
    }

    override suspend fun subirMedicamento(actividad: Actividad, detalle: ActividadDetalle.Medicamento) {
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
            "detalleNombreMedicamento" to detalle.nombreMedicamento,
            "detalleDosis" to detalle.dosis,
            "detalleModoFrecuencia" to detalle.modoFrecuencia.name,
            "detalleIntervaloHoras" to detalle.intervaloHoras,
            "detalleHoraPrimeraDosis" to detalle.horaPrimeraDosis,
            "detalleHorariosCalculados" to detalle.horariosCalculados,
            "detalleComidasRelacionadas" to detalle.comidasRelacionadas.map { mapOf("comida" to it.comida.name, "momento" to it.momento.name) },
            "detalleFechaInicio" to detalle.fechaInicio,
            "detalleFechaFin" to detalle.fechaFin,
            "detalleCantidadDosisTotal" to detalle.cantidadDosisTotal,
            "detalleNivelRecordatorio" to detalle.nivelRecordatorio.name,
            "detalleRecordatorioPersistente" to detalle.recordatorioPersistente,
            "detalleIntervaloPersistenciaMin" to detalle.intervaloPersistenciaMin,
        )
        coleccion.document(actividad.id).set(datos).await()
    }

    override suspend fun restaurarMedicamentos(): List<Pair<Actividad, ActividadDetalle.Medicamento>> {
        val coleccion = coleccionActividades() ?: return emptyList()
        val snapshot = coleccion.whereEqualTo("tipo", TipoActividad.MEDICAMENTO.name).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val actividad = actividadBaseDesde(doc, TipoActividad.MEDICAMENTO)
            val comidas = (doc.get("detalleComidasRelacionadas") as? List<*>)?.mapNotNull { raw ->
                val mapa = raw as? Map<*, *> ?: return@mapNotNull null
                val comida = (mapa["comida"] as? String)?.let { runCatching { Comida.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
                val momento = (mapa["momento"] as? String)?.let { runCatching { MomentoRelativoComida.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
                ComidaRelacionada(comida, momento)
            } ?: emptyList()
            val detalle = ActividadDetalle.Medicamento(
                nombreMedicamento = doc.getString("detalleNombreMedicamento") ?: "",
                dosis = doc.getString("detalleDosis") ?: "",
                modoFrecuencia = runCatching { ModoFrecuenciaMedicamento.valueOf(doc.getString("detalleModoFrecuencia") ?: "") }
                    .getOrDefault(ModoFrecuenciaMedicamento.INTERVALO_HORAS),
                intervaloHoras = doc.getLong("detalleIntervaloHoras")?.toInt(),
                horaPrimeraDosis = doc.getString("detalleHoraPrimeraDosis"),
                horariosCalculados = (doc.get("detalleHorariosCalculados") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                comidasRelacionadas = comidas,
                fechaInicio = doc.getLong("detalleFechaInicio") ?: 0L,
                fechaFin = doc.getLong("detalleFechaFin"),
                cantidadDosisTotal = doc.getLong("detalleCantidadDosisTotal")?.toInt(),
                nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(doc.getString("detalleNivelRecordatorio") ?: "") }
                    .getOrDefault(NivelRecordatorio.SONIDO),
                recordatorioPersistente = doc.getBoolean("detalleRecordatorioPersistente") ?: false,
                intervaloPersistenciaMin = doc.getLong("detalleIntervaloPersistenciaMin")?.toInt(),
            )
            actividad to detalle
        }
    }

    override suspend fun subirTomaMedicamento(actividadId: String, fecha: Long, horario: String, estado: EstadoActividad) {
        val coleccion = coleccion(SUBCOLECCION_TOMAS) ?: return
        val datos = mapOf("actividadId" to actividadId, "fecha" to fecha, "horario" to horario, "estado" to estado.name)
        coleccion.document("${actividadId}_${fecha}_$horario").set(datos).await()
    }

    override suspend fun restaurarTomasMedicamento(): List<TomaMedicamentoRemota> {
        val coleccion = coleccion(SUBCOLECCION_TOMAS) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            TomaMedicamentoRemota(
                actividadId = doc.getString("actividadId") ?: return@mapNotNull null,
                fecha = doc.getLong("fecha") ?: return@mapNotNull null,
                horario = doc.getString("horario") ?: return@mapNotNull null,
                estado = runCatching { EstadoActividad.valueOf(doc.getString("estado") ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR),
            )
        }
    }

    override suspend fun subirCita(actividad: Actividad, detalle: ActividadDetalle.Cita) {
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
            "detalleLugar" to detalle.lugar,
            "detalleMotivo" to detalle.motivo,
            "detalleFechaHora" to detalle.fechaHora,
            "detalleRecordatorios" to detalle.recordatorios.map { mapOf("anticipacion" to it.anticipacion.name, "hora" to it.hora) },
            "detalleNivelRecordatorio" to detalle.nivelRecordatorio.name,
            "detalleEsCurso" to detalle.esCurso,
            "detalleDiasSemana" to detalle.diasSemana.toList(),
            "detalleHoraSesion" to detalle.horaSesion,
            "detalleFechaInicioCurso" to detalle.fechaInicioCurso,
            "detalleCantidadSesionesTotal" to detalle.cantidadSesionesTotal,
        )
        coleccion.document(actividad.id).set(datos).await()
    }

    override suspend fun restaurarCitas(): List<Pair<Actividad, ActividadDetalle.Cita>> {
        val coleccion = coleccionActividades() ?: return emptyList()
        val snapshot = coleccion.whereEqualTo("tipo", TipoActividad.CITA.name).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val actividad = actividadBaseDesde(doc, TipoActividad.CITA)
            val recordatorios = (doc.get("detalleRecordatorios") as? List<*>)?.mapNotNull { raw ->
                val mapa = raw as? Map<*, *> ?: return@mapNotNull null
                val anticipacion = (mapa["anticipacion"] as? String)?.let { runCatching { AnticipacionRecordatorio.valueOf(it) }.getOrNull() } ?: return@mapNotNull null
                val hora = mapa["hora"] as? String ?: return@mapNotNull null
                RecordatorioCita(anticipacion, hora)
            } ?: emptyList()
            val detalle = ActividadDetalle.Cita(
                lugar = doc.getString("detalleLugar"),
                motivo = doc.getString("detalleMotivo"),
                fechaHora = doc.getLong("detalleFechaHora") ?: 0L,
                recordatorios = recordatorios,
                nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(doc.getString("detalleNivelRecordatorio") ?: "") }
                    .getOrDefault(NivelRecordatorio.SONIDO),
                esCurso = doc.getBoolean("detalleEsCurso") ?: false,
                diasSemana = (doc.get("detalleDiasSemana") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet() ?: emptySet(),
                horaSesion = doc.getString("detalleHoraSesion"),
                fechaInicioCurso = doc.getLong("detalleFechaInicioCurso"),
                cantidadSesionesTotal = doc.getLong("detalleCantidadSesionesTotal")?.toInt(),
            )
            actividad to detalle
        }
    }

    override suspend fun subirSesionCita(sesion: SesionCita) {
        val coleccion = coleccion(SUBCOLECCION_SESIONES_CITA) ?: return
        val datos = mapOf(
            "actividadId" to sesion.actividadId,
            "numeroSesion" to sesion.numeroSesion,
            "fecha" to sesion.fecha,
            "fechaOriginal" to sesion.fechaOriginal,
            "horario" to sesion.horario,
            "estado" to sesion.estado.name,
        )
        coleccion.document(sesion.id).set(datos).await()
    }

    override suspend fun restaurarSesionesCita(): List<SesionCita> {
        val coleccion = coleccion(SUBCOLECCION_SESIONES_CITA) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            SesionCita(
                id = doc.id,
                actividadId = doc.getString("actividadId") ?: return@mapNotNull null,
                numeroSesion = doc.getLong("numeroSesion")?.toInt() ?: return@mapNotNull null,
                fecha = doc.getLong("fecha") ?: return@mapNotNull null,
                fechaOriginal = doc.getLong("fechaOriginal") ?: 0L,
                horario = doc.getString("horario") ?: "",
                estado = runCatching { EstadoActividad.valueOf(doc.getString("estado") ?: "") }.getOrDefault(EstadoActividad.SIN_CONFIRMAR),
            )
        }
    }

    override suspend fun subirFechaImportante(actividad: Actividad, detalle: ActividadDetalle.FechaImportante) {
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
            "detalleRecurrencia" to detalle.recurrencia.name,
            "detalleFechaBase" to detalle.fechaBase,
            "detalleHoraNotificacion" to detalle.horaNotificacion,
            "detalleAnticipacion" to detalle.anticipacion.name,
            "detalleTipoAviso" to detalle.tipoAviso.name,
        )
        coleccion.document(actividad.id).set(datos).await()
    }

    override suspend fun restaurarFechasImportantes(): List<Pair<Actividad, ActividadDetalle.FechaImportante>> {
        val coleccion = coleccionActividades() ?: return emptyList()
        val snapshot = coleccion.whereEqualTo("tipo", TipoActividad.FECHA_IMPORTANTE.name).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val actividad = actividadBaseDesde(doc, TipoActividad.FECHA_IMPORTANTE)
            val detalle = ActividadDetalle.FechaImportante(
                recurrencia = runCatching { Recurrencia.valueOf(doc.getString("detalleRecurrencia") ?: "") }.getOrDefault(Recurrencia.ANUAL),
                fechaBase = doc.getLong("detalleFechaBase") ?: 0L,
                horaNotificacion = doc.getString("detalleHoraNotificacion") ?: "09:00",
                anticipacion = runCatching { AnticipacionRecordatorio.valueOf(doc.getString("detalleAnticipacion") ?: "") }
                    .getOrDefault(AnticipacionRecordatorio.MISMO_DIA),
                tipoAviso = runCatching { TipoAviso.valueOf(doc.getString("detalleTipoAviso") ?: "") }.getOrDefault(TipoAviso.MENSAJE_SILENCIOSO),
            )
            actividad to detalle
        }
    }

    override suspend fun subirActividadSegunTipo(actividad: Actividad) {
        when (val detalle = actividad.detalle) {
            is ActividadDetalle.Habito -> subirHabito(actividad, detalle)
            is ActividadDetalle.Tarea -> subirTarea(actividad, detalle)
            is ActividadDetalle.Rutina -> subirRutina(actividad, detalle)
            is ActividadDetalle.Medicamento -> subirMedicamento(actividad, detalle)
            is ActividadDetalle.Cita -> subirCita(actividad, detalle)
            is ActividadDetalle.FechaImportante -> subirFechaImportante(actividad, detalle)
            null -> Unit
        }
    }

    override suspend fun eliminarActividad(actividadId: String) {
        coleccionActividades()?.document(actividadId)?.delete()?.await()
        coleccionRegistros()?.let { col ->
            col.whereEqualTo("actividadId", actividadId).get().await().documents.forEach { it.reference.delete().await() }
        }
        coleccion(SUBCOLECCION_TOMAS)?.let { col ->
            col.whereEqualTo("actividadId", actividadId).get().await().documents.forEach { it.reference.delete().await() }
        }
        coleccion(SUBCOLECCION_SESIONES_CITA)?.let { col ->
            col.whereEqualTo("actividadId", actividadId).get().await().documents.forEach { it.reference.delete().await() }
        }
    }

    override suspend fun subirMovimientoFinanciero(movimiento: MovimientoFinanciero) {
        val coleccion = coleccion(SUBCOLECCION_FINANZAS) ?: return
        val datos = mapOf(
            "tipo" to movimiento.tipo.name,
            "monto" to movimiento.monto,
            "categoria" to movimiento.categoria,
            "descripcion" to movimiento.descripcion,
            "fecha" to movimiento.fecha,
            "privacidad" to movimiento.privacidad.name,
        )
        coleccion.document(movimiento.id).set(datos).await()
    }

    override suspend fun eliminarMovimientoFinanciero(movimientoId: String) {
        coleccion(SUBCOLECCION_FINANZAS)?.document(movimientoId)?.delete()?.await()
    }

    override suspend fun restaurarMovimientosFinancieros(): List<MovimientoFinanciero> {
        val coleccion = coleccion(SUBCOLECCION_FINANZAS) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            MovimientoFinanciero(
                id = doc.id,
                espacioId = "",
                tipo = runCatching { TipoMovimientoFinanciero.valueOf(doc.getString("tipo") ?: "") }
                    .getOrDefault(TipoMovimientoFinanciero.EGRESO),
                monto = doc.getDouble("monto") ?: 0.0,
                categoria = doc.getString("categoria") ?: "",
                descripcion = doc.getString("descripcion"),
                fecha = doc.getLong("fecha") ?: 0L,
                privacidad = runCatching { Privacidad.valueOf(doc.getString("privacidad") ?: "") }.getOrDefault(Privacidad.SOLO_YO),
            )
        }
    }

    override suspend fun subirEntradaDiario(entrada: EntradaDiario) {
        val coleccion = coleccion(SUBCOLECCION_DIARIO) ?: return
        val datos = mapOf(
            "propietario" to entrada.propietario,
            "titulo" to entrada.titulo,
            "texto" to entrada.texto,
            "areaDeVidaId" to entrada.areaDeVidaId,
            "fecha" to entrada.fecha,
            "privacidad" to entrada.privacidad.name,
            "fotos" to entrada.fotos,
        )
        coleccion.document(entrada.id).set(datos).await()
    }

    override suspend fun eliminarEntradaDiario(entradaId: String) {
        coleccion(SUBCOLECCION_DIARIO)?.document(entradaId)?.delete()?.await()
    }

    override suspend fun restaurarEntradasDiario(): List<EntradaDiario> {
        val coleccion = coleccion(SUBCOLECCION_DIARIO) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            EntradaDiario(
                id = doc.id,
                espacioId = "",
                propietario = doc.getString("propietario") ?: "",
                titulo = doc.getString("titulo"),
                texto = doc.getString("texto") ?: "",
                areaDeVidaId = doc.getString("areaDeVidaId"),
                fecha = doc.getLong("fecha") ?: 0L,
                privacidad = runCatching { Privacidad.valueOf(doc.getString("privacidad") ?: "") }.getOrDefault(Privacidad.SOLO_YO),
                fotos = (doc.get("fotos") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            )
        }
    }

    override suspend fun subirNota(nota: Nota) {
        val coleccion = coleccion(SUBCOLECCION_NOTAS) ?: return
        val datos = mapOf(
            "propietario" to nota.propietario,
            "titulo" to nota.titulo,
            "contenido" to nota.contenido,
            "fechaCreacion" to nota.fechaCreacion,
            "fechaEdicion" to nota.fechaEdicion,
            "orden" to nota.orden,
        )
        coleccion.document(nota.id).set(datos).await()
    }

    override suspend fun eliminarNota(notaId: String) {
        coleccion(SUBCOLECCION_NOTAS)?.document(notaId)?.delete()?.await()
    }

    override suspend fun restaurarNotas(): List<Nota> {
        val coleccion = coleccion(SUBCOLECCION_NOTAS) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            Nota(
                id = doc.id,
                espacioId = "",
                propietario = doc.getString("propietario") ?: "",
                titulo = doc.getString("titulo"),
                contenido = doc.getString("contenido") ?: "",
                fechaCreacion = doc.getLong("fechaCreacion") ?: 0L,
                fechaEdicion = doc.getLong("fechaEdicion") ?: 0L,
                orden = doc.getLong("orden")?.toInt() ?: 0,
            )
        }
    }

    override suspend fun subirMeta(meta: Meta) {
        val coleccion = coleccion(SUBCOLECCION_METAS) ?: return
        val datos = mapOf(
            "nombre" to meta.nombre,
            "areaDeVidaId" to meta.areaDeVidaId,
            "fechaLimite" to meta.fechaLimite,
            "comoSeMide" to meta.comoSeMide.name,
            "valorObjetivo" to meta.valorObjetivo,
            "valorActual" to meta.valorActual,
            "actividadesVinculadasIds" to meta.actividadesVinculadasIds,
            "ultimoHitoCelebrado" to meta.ultimoHitoCelebrado,
            "categoria" to meta.categoria?.name,
            "nivelRecordatorio" to meta.nivelRecordatorio.name,
        )
        coleccion.document(meta.id).set(datos).await()
    }

    override suspend fun eliminarMeta(metaId: String) {
        coleccion(SUBCOLECCION_METAS)?.document(metaId)?.delete()?.await()
    }

    override suspend fun restaurarMetas(): List<Meta> {
        val coleccion = coleccion(SUBCOLECCION_METAS) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            Meta(
                id = doc.id,
                espacioId = "",
                nombre = doc.getString("nombre") ?: "",
                areaDeVidaId = doc.getString("areaDeVidaId"),
                fechaLimite = doc.getLong("fechaLimite"),
                comoSeMide = runCatching { ComoSeMideMeta.valueOf(doc.getString("comoSeMide") ?: "") }
                    .getOrDefault(ComoSeMideMeta.MANUAL),
                valorObjetivo = doc.getDouble("valorObjetivo") ?: 0.0,
                valorActual = doc.getDouble("valorActual") ?: 0.0,
                actividadesVinculadasIds = (doc.get("actividadesVinculadasIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                ultimoHitoCelebrado = doc.getLong("ultimoHitoCelebrado")?.toInt() ?: 0,
                categoria = doc.getString("categoria")?.let { runCatching { CategoriaMeta.valueOf(it) }.getOrNull() },
                nivelRecordatorio = runCatching { NivelRecordatorio.valueOf(doc.getString("nivelRecordatorio") ?: "") }
                    .getOrDefault(NivelRecordatorio.SONIDO),
            )
        }
    }

    override suspend fun subirLista(lista: ListaConItems) {
        val coleccion = coleccion(SUBCOLECCION_LISTAS) ?: return
        val datos = mapOf(
            "nombre" to lista.nombre,
            "items" to lista.items.map { item ->
                mapOf("id" to item.id, "texto" to item.texto, "marcado" to item.marcado, "orden" to item.orden)
            },
        )
        coleccion.document(lista.id).set(datos).await()
    }

    override suspend fun eliminarLista(listaId: String) {
        coleccion(SUBCOLECCION_LISTAS)?.document(listaId)?.delete()?.await()
    }

    override suspend fun restaurarListas(): List<ListaConItems> {
        val coleccion = coleccion(SUBCOLECCION_LISTAS) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            val items = (doc.get("items") as? List<*>)?.mapNotNull { raw ->
                val mapa = raw as? Map<*, *> ?: return@mapNotNull null
                ListaItem(
                    id = mapa["id"] as? String ?: return@mapNotNull null,
                    listaId = doc.id,
                    texto = mapa["texto"] as? String ?: "",
                    marcado = mapa["marcado"] as? Boolean ?: false,
                    orden = (mapa["orden"] as? Number)?.toInt() ?: 0,
                )
            } ?: emptyList()
            ListaConItems(id = doc.id, nombre = doc.getString("nombre") ?: "", items = items)
        }
    }

    override suspend fun subirProposito(proposito: PropositoPersonal) {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return
        val datos = mapOf("respuestas" to proposito.respuestas, "fechaEdicion" to proposito.fechaEdicion)
        firestore.collection(COLECCION_USUARIOS).document(miFirebaseUid)
            .collection(SUBCOLECCION_PROPOSITO).document("unico").set(datos).await()
    }

    override suspend fun restaurarProposito(): PropositoPersonal? {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return null
        val doc = firestore.collection(COLECCION_USUARIOS).document(miFirebaseUid)
            .collection(SUBCOLECCION_PROPOSITO).document("unico").get().await()
        if (!doc.exists()) return null
        @Suppress("UNCHECKED_CAST")
        val respuestas = (doc.get("respuestas") as? Map<String, String>) ?: emptyMap()
        return PropositoPersonal(
            espacioId = "",
            propietario = "",
            respuestas = respuestas,
            fechaEdicion = doc.getLong("fechaEdicion") ?: 0L,
        )
    }

    override suspend fun subirRegistroDiario(registro: RegistroDiario) {
        val coleccion = coleccion(SUBCOLECCION_REGISTROS_DIARIOS) ?: return
        val datos = mapOf(
            "fecha" to registro.fecha,
            "actividadesCompletadas" to registro.actividadesCompletadas,
            "actividadesTotales" to registro.actividadesTotales,
            "puntuacion" to registro.puntuacion,
            "estadoAnimo" to registro.estadoAnimo,
            "queLogre" to registro.queLogre,
            "queCosto" to registro.queCosto,
            "queAjusto" to registro.queAjusto,
        )
        coleccion.document(registro.id).set(datos).await()
    }

    override suspend fun restaurarRegistrosDiarios(): List<RegistroDiario> {
        val coleccion = coleccion(SUBCOLECCION_REGISTROS_DIARIOS) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            RegistroDiario(
                id = doc.id,
                espacioId = "",
                fecha = doc.getLong("fecha") ?: return@mapNotNull null,
                actividadesCompletadas = doc.getLong("actividadesCompletadas")?.toInt() ?: 0,
                actividadesTotales = doc.getLong("actividadesTotales")?.toInt() ?: 0,
                puntuacion = doc.getLong("puntuacion")?.toInt() ?: 0,
                estadoAnimo = doc.getString("estadoAnimo"),
                queLogre = doc.getString("queLogre"),
                queCosto = doc.getString("queCosto"),
                queAjusto = doc.getString("queAjusto"),
            )
        }
    }

    override suspend fun subirRegistroSemanal(registro: RegistroSemanal) {
        val coleccion = coleccion(SUBCOLECCION_REGISTROS_SEMANALES) ?: return
        val datos = mapOf(
            "semana" to registro.semana,
            "cumplimientoGeneralPorcentaje" to registro.cumplimientoGeneralPorcentaje,
            "rachaMaxima" to registro.rachaMaxima,
            "queLogre" to registro.queLogre,
            "queNoFunciono" to registro.queNoFunciono,
            "queAjusto" to registro.queAjusto,
        )
        coleccion.document(registro.id).set(datos).await()
    }

    override suspend fun restaurarRegistrosSemanales(): List<RegistroSemanal> {
        val coleccion = coleccion(SUBCOLECCION_REGISTROS_SEMANALES) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            RegistroSemanal(
                id = doc.id,
                espacioId = "",
                semana = doc.getString("semana") ?: return@mapNotNull null,
                cumplimientoGeneralPorcentaje = doc.getLong("cumplimientoGeneralPorcentaje")?.toInt() ?: 0,
                rachaMaxima = doc.getLong("rachaMaxima")?.toInt() ?: 0,
                queLogre = doc.getString("queLogre"),
                queNoFunciono = doc.getString("queNoFunciono"),
                queAjusto = doc.getString("queAjusto"),
            )
        }
    }

    override suspend fun subirNotificacion(notificacion: Notificacion) {
        val coleccion = coleccion(SUBCOLECCION_NOTIFICACIONES) ?: return
        val datos = mapOf(
            "emoji" to notificacion.emoji,
            "titulo" to notificacion.titulo,
            "cuerpo" to notificacion.cuerpo,
            "fecha" to notificacion.fecha,
            "leido" to notificacion.leido,
            "solicitudId" to notificacion.solicitudId,
        )
        coleccion.document(notificacion.id).set(datos).await()
    }

    override suspend fun marcarNotificacionLeidaRemota(notificacionId: String) {
        val coleccion = coleccion(SUBCOLECCION_NOTIFICACIONES) ?: return
        coleccion.document(notificacionId).update("leido", true).await()
    }

    override suspend fun restaurarNotificaciones(): List<Notificacion> {
        val coleccion = coleccion(SUBCOLECCION_NOTIFICACIONES) ?: return emptyList()
        return coleccion.get().await().documents.mapNotNull { doc ->
            Notificacion(
                id = doc.id,
                emoji = doc.getString("emoji") ?: return@mapNotNull null,
                titulo = doc.getString("titulo") ?: return@mapNotNull null,
                cuerpo = doc.getString("cuerpo") ?: "",
                fecha = doc.getLong("fecha") ?: 0L,
                leido = doc.getBoolean("leido") ?: false,
                solicitudId = doc.getString("solicitudId"),
            )
        }
    }
}
