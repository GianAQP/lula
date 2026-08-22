package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.CategoriaMeta
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.ListaConItems
import com.aqpseller.lulaapp.domain.model.ListaItem
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
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
private const val SUBCOLECCION_FINANZAS = "movimientosFinancieros"
private const val SUBCOLECCION_DIARIO = "entradasDiario"
private const val SUBCOLECCION_NOTAS = "notas"
private const val SUBCOLECCION_METAS = "metas"
private const val SUBCOLECCION_LISTAS = "listas"
private const val SUBCOLECCION_PROPOSITO = "proposito"

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
}
