package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.data.local.dao.ActividadDao
import com.aqpseller.lulaapp.data.local.dao.CitaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.FechaImportanteDetalleDao
import com.aqpseller.lulaapp.data.local.dao.HabitoDetalleDao
import com.aqpseller.lulaapp.data.local.dao.HistorialCambiosDao
import com.aqpseller.lulaapp.data.local.dao.MedicamentoDetalleDao
import com.aqpseller.lulaapp.data.local.dao.RegistroActividadDao
import com.aqpseller.lulaapp.data.local.dao.RutinaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.SesionCitaDao
import com.aqpseller.lulaapp.data.local.dao.TareaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.TomaMedicamentoDao
import com.aqpseller.lulaapp.data.local.entity.ActividadEntity
import com.aqpseller.lulaapp.data.local.entity.HabitoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.RegistroActividadEntity
import com.aqpseller.lulaapp.data.local.entity.TomaMedicamentoEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.DiaHistorialHabito
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.EstadoActividadEnFecha
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SesionCita
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ActividadRepositoryImpl @Inject constructor(
    private val actividadDao: ActividadDao,
    private val habitoDetalleDao: HabitoDetalleDao,
    private val tareaDetalleDao: TareaDetalleDao,
    private val rutinaDetalleDao: RutinaDetalleDao,
    private val medicamentoDetalleDao: MedicamentoDetalleDao,
    private val citaDetalleDao: CitaDetalleDao,
    private val fechaImportanteDetalleDao: FechaImportanteDetalleDao,
    private val tomaMedicamentoDao: TomaMedicamentoDao,
    private val sesionCitaDao: SesionCitaDao,
    private val registroActividadDao: RegistroActividadDao,
    private val historialCambiosDao: HistorialCambiosDao,
    private val auditLogger: AuditLogger,
) : ActividadRepository {

    override suspend fun crearHabito(actividad: Actividad, detalle: ActividadDetalle.Habito, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        habitoDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun crearTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        tareaDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun crearRutina(actividad: Actividad, detalle: ActividadDetalle.Rutina, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        rutinaDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun crearMedicamento(actividad: Actividad, detalle: ActividadDetalle.Medicamento, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        medicamentoDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun crearCita(actividad: Actividad, detalle: ActividadDetalle.Cita, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        citaDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun crearFechaImportante(actividad: Actividad, detalle: ActividadDetalle.FechaImportante, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        fechaImportanteDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    /**
     * Para HABITO, el cumplimiento se guarda por día en `registro_actividad` (una actividad
     * recurrente no tiene un solo "estado" de por vida) — para TAREA, el estado vive
     * directamente en `ActividadEntity` (ver `Plan/08-decisiones-tecnicas.md`).
     */
    override suspend fun marcarEstado(id: String, estado: EstadoActividad, usuarioId: String, fechaCompletado: Long?) {
        val actual = actividadDao.obtenerPorId(id) ?: return
        if (actual.tipo == TipoActividad.HABITO.name) {
            val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
            val registroExistente = registroActividadDao.obtenerPorActividadIdYFecha(id, fechaHoy)
            val registro = RegistroActividadEntity(
                id = registroExistente?.id ?: IdGenerator.newId(),
                actividadId = id,
                fecha = fechaHoy,
                estado = estado.name,
            )
            registroActividadDao.upsert(registro)
            auditLogger.registrar<RegistroActividadEntity>(
                entidad = "registro_actividad",
                entidadId = registro.id,
                accion = if (registroExistente == null) AccionAuditoria.CREAR else AccionAuditoria.ACTUALIZAR,
                antes = registroExistente,
                despues = registro,
                usuarioId = usuarioId,
            )
        } else {
            // Solo tiene sentido para Tarea puntual: para Habito el estado por día vive en
            // registro_actividad (ver arriba); Rutina/Medicamento/Cita/Fecha importante no
            // exponen "completado el {fecha}" en ninguna pantalla todavía.
            val fechaCompletadoFinal = if (estado == EstadoActividad.CONFIRMADO) {
                fechaCompletado ?: DateTimeUtils.ahoraEpochMillis()
            } else {
                null
            }
            actividadDao.actualizarEstadoYFechaCompletado(id, estado.name, fechaCompletadoFinal)
            auditLogger.registrar<ActividadEntity>(
                entidad = "actividad",
                entidadId = id,
                accion = AccionAuditoria.ACTUALIZAR,
                antes = actual,
                despues = actual.copy(estado = estado.name, fechaCompletado = fechaCompletadoFinal),
                usuarioId = usuarioId,
            )
        }
    }

    override suspend fun agregarPermisoCompartido(actividadId: String, concederA: String, permiso: PermisoCompartir, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        val puedeVer = (decodeStringList(actual.puedeVerJson) + concederA).distinct()
        val puedeRecordar = if (permiso == PermisoCompartir.PUEDE_VER_Y_RECORDAR) {
            (decodeStringList(actual.puedeRecordarJson) + concederA).distinct()
        } else {
            decodeStringList(actual.puedeRecordarJson)
        }
        val actualizado = actual.copy(puedeVerJson = encodeStringList(puedeVer), puedeRecordarJson = encodeStringList(puedeRecordar))
        actividadDao.upsert(actualizado)
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun mergeTareaRemota(actividad: Actividad, detalle: ActividadDetalle.Tarea) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        tareaDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.ACTUALIZAR,
            despues = entity,
            usuarioId = actividad.propietario,
        )
    }

    override suspend fun mergeHabitoRemoto(actividad: Actividad, detalle: ActividadDetalle.Habito) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        habitoDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.ACTUALIZAR,
            despues = entity,
            usuarioId = actividad.propietario,
        )
    }

    override suspend fun mergeRegistroHabitoRemoto(actividadId: String, fecha: Long, estado: EstadoActividad) {
        val existente = registroActividadDao.obtenerPorActividadIdYFecha(actividadId, fecha)
        registroActividadDao.upsert(
            RegistroActividadEntity(
                id = existente?.id ?: IdGenerator.newId(),
                actividadId = actividadId,
                fecha = fecha,
                estado = estado.name,
            ),
        )
    }

    override suspend fun mergeRutinaRemota(actividad: Actividad, detalle: ActividadDetalle.Rutina) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        rutinaDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.ACTUALIZAR,
            despues = entity,
            usuarioId = actividad.propietario,
        )
    }

    override suspend fun mergeMedicamentoRemoto(actividad: Actividad, detalle: ActividadDetalle.Medicamento) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        medicamentoDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.ACTUALIZAR,
            despues = entity,
            usuarioId = actividad.propietario,
        )
    }

    override suspend fun mergeCitaRemota(actividad: Actividad, detalle: ActividadDetalle.Cita) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        citaDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.ACTUALIZAR,
            despues = entity,
            usuarioId = actividad.propietario,
        )
    }

    override suspend fun mergeFechaImportanteRemota(actividad: Actividad, detalle: ActividadDetalle.FechaImportante) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        fechaImportanteDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.ACTUALIZAR,
            despues = entity,
            usuarioId = actividad.propietario,
        )
    }

    override suspend fun mergeTomaMedicamentoRemota(actividadId: String, fecha: Long, horario: String, estado: EstadoActividad) {
        val existente = tomaMedicamentoDao.obtenerPorActividadIdYFecha(actividadId, fecha).firstOrNull { it.horario == horario }
        tomaMedicamentoDao.upsert(
            TomaMedicamentoEntity(
                id = existente?.id ?: IdGenerator.newId(),
                actividadId = actividadId,
                fecha = fecha,
                horario = horario,
                estado = estado.name,
            ),
        )
    }

    override suspend fun actualizarHabito(actividadId: String, nombre: String, detalle: ActividadDetalle.Habito, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        val actualizado = actual.copy(nombre = nombre, momentoDelDia = detalle.momentoDelDia.name)
        actividadDao.upsert(actualizado)
        habitoDetalleDao.upsert(detalle.toEntity(actividadId))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarTarea(actividadId: String, nombre: String, detalle: ActividadDetalle.Tarea, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        val actualizado = actual.copy(nombre = nombre)
        actividadDao.upsert(actualizado)
        tareaDetalleDao.upsert(detalle.toEntity(actividadId))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarRutina(actividadId: String, nombre: String, detalle: ActividadDetalle.Rutina, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        val actualizado = actual.copy(nombre = nombre, momentoDelDia = detalle.momentoDelDia.name)
        actividadDao.upsert(actualizado)
        rutinaDetalleDao.upsert(detalle.toEntity(actividadId))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarMedicamento(actividadId: String, nombre: String, detalle: ActividadDetalle.Medicamento, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        val actualizado = actual.copy(nombre = nombre)
        actividadDao.upsert(actualizado)
        medicamentoDetalleDao.upsert(detalle.toEntity(actividadId))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarCita(actividadId: String, nombre: String, detalle: ActividadDetalle.Cita, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        val actualizado = actual.copy(nombre = nombre)
        actividadDao.upsert(actualizado)
        citaDetalleDao.upsert(detalle.toEntity(actividadId))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarFechaImportante(actividadId: String, nombre: String, detalle: ActividadDetalle.FechaImportante, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        val actualizado = actual.copy(nombre = nombre)
        actividadDao.upsert(actualizado)
        fechaImportanteDetalleDao.upsert(detalle.toEntity(actividadId))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun pausarReanudar(actividadId: String, activa: Boolean, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        actividadDao.actualizarActiva(actividadId, activa)
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actual.copy(activa = activa),
            usuarioId = usuarioId,
        )
    }

    override suspend fun reprogramarTareaRecurrente(actividadId: String, nuevaFechaLimite: Long, usuarioId: String) {
        val detalleActual = tareaDetalleDao.obtenerPorActividadId(actividadId) ?: return

        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val registro = RegistroActividadEntity(
            id = IdGenerator.newId(),
            actividadId = actividadId,
            fecha = fechaHoy,
            estado = EstadoActividad.CONFIRMADO.name,
        )
        registroActividadDao.upsert(registro)
        auditLogger.registrar<RegistroActividadEntity>(
            entidad = "registro_actividad",
            entidadId = registro.id,
            accion = AccionAuditoria.CREAR,
            despues = registro,
            usuarioId = usuarioId,
        )

        tareaDetalleDao.upsert(detalleActual.copy(fechaLimite = nuevaFechaLimite))

        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        actividadDao.actualizarEstado(actividadId, EstadoActividad.SIN_CONFIRMAR.name)
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actual.copy(estado = EstadoActividad.SIN_CONFIRMAR.name),
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarProgresionHabito(actividadId: String, duracionActualMin: Int, proximaRevisionEpochDay: Long, usuarioId: String) {
        val detalleActual = habitoDetalleDao.obtenerPorActividadId(actividadId) ?: return
        val detalleActualizado = detalleActual.copy(
            duracionActualMin = duracionActualMin,
            proximaRevisionEpochDay = proximaRevisionEpochDay,
        )
        habitoDetalleDao.upsert(detalleActualizado)
        auditLogger.registrar<HabitoDetalleEntity>(
            entidad = "habito_detalle",
            entidadId = actividadId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = detalleActual,
            despues = detalleActualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun eliminar(actividadId: String, usuarioId: String) {
        val actual = actividadDao.obtenerPorId(actividadId) ?: return
        actividadDao.eliminar(actividadId)
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividadId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerConDetalle(actividadId: String): Actividad? {
        val entity = actividadDao.obtenerPorId(actividadId) ?: return null
        return resolverConDetalle(listOf(entity)).firstOrNull()
    }

    override suspend fun obtenerTareasVinculadasA(actividadVinculadaId: String): List<Actividad> {
        val tareaIds = tareaDetalleDao.obtenerPorActividadVinculadaId(actividadVinculadaId).map { it.actividadId }
        if (tareaIds.isEmpty()) return emptyList()
        val entidades = tareaIds.mapNotNull { actividadDao.obtenerPorId(it) }
        return resolverConDetalle(entidades)
    }

    /**
     * `combine` con `registroActividadDao.observarPorFecha` es necesario aunque no se use su
     * valor directamente: es la única forma de que este Flow se vuelva a emitir cuando se
     * marca/desmarca un hábito hoy (esa escritura va a `registro_actividad`, una tabla que
     * Room no asocia con la consulta de `actividad` por su cuenta). Ver
     * `Plan/08-decisiones-tecnicas.md`.
     */
    override fun observarActividadesDeEspacio(espacioId: String): Flow<List<Actividad>> {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        return combine(
            actividadDao.observarActivasDeEspacio(espacioId),
            registroActividadDao.observarPorFecha(fechaHoy),
        ) { entidades, _ -> entidades }
            .map { resolverConDetalle(it) }
    }

    override fun observarHabitos(espacioId: String): Flow<List<Actividad>> {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        return combine(
            actividadDao.observarPorTipo(espacioId, TipoActividad.HABITO.name),
            registroActividadDao.observarPorFecha(fechaHoy),
        ) { entidades, _ -> entidades }
            .map { resolverConDetalle(it) }
    }

    override fun observarTareas(espacioId: String): Flow<List<Actividad>> =
        actividadDao.observarPorTipo(espacioId, TipoActividad.TAREA.name).map { resolverConDetalle(it) }

    override fun observarRutinas(espacioId: String): Flow<List<Actividad>> =
        actividadDao.observarPorTipo(espacioId, TipoActividad.RUTINA.name).map { resolverConDetalle(it) }

    override fun observarMedicamentos(espacioId: String): Flow<List<Actividad>> =
        actividadDao.observarPorTipo(espacioId, TipoActividad.MEDICAMENTO.name).map { resolverConDetalle(it) }

    override fun observarCitas(espacioId: String): Flow<List<Actividad>> =
        actividadDao.observarPorTipo(espacioId, TipoActividad.CITA.name).map { resolverConDetalle(it) }

    override fun observarFechasImportantes(espacioId: String): Flow<List<Actividad>> =
        actividadDao.observarPorTipo(espacioId, TipoActividad.FECHA_IMPORTANTE.name).map { resolverConDetalle(it) }

    override suspend fun marcarToma(actividadId: String, horario: String, estado: EstadoActividad, usuarioId: String) {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val existente = tomaMedicamentoDao.obtenerPorActividadIdYFecha(actividadId, fechaHoy).firstOrNull { it.horario == horario }
        val toma = TomaMedicamentoEntity(
            id = existente?.id ?: IdGenerator.newId(),
            actividadId = actividadId,
            fecha = fechaHoy,
            horario = horario,
            estado = estado.name,
        )
        tomaMedicamentoDao.upsert(toma)
        auditLogger.registrar<TomaMedicamentoEntity>(
            entidad = "toma_medicamento",
            entidadId = toma.id,
            accion = if (existente == null) AccionAuditoria.CREAR else AccionAuditoria.ACTUALIZAR,
            antes = existente,
            despues = toma,
            usuarioId = usuarioId,
        )
    }

    override fun observarTomasDeHoy(actividadIds: List<String>): Flow<List<TomaMedicamento>> {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        return tomaMedicamentoDao.observarPorFecha(fechaHoy)
            .map { tomas -> tomas.filter { it.actividadId in actividadIds }.map { it.toDomain() } }
    }

    override suspend fun obtenerHistorialTomas(actividadId: String, dias: Int): List<TomaMedicamento> {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val fechaDesde = fechaHoy - (dias - 1)
        return tomaMedicamentoDao.obtenerPorActividadIdYRango(actividadId, fechaDesde, fechaHoy).map { it.toDomain() }
    }

    override suspend fun obtenerEstadosDeRango(actividadIds: List<String>, desde: Long, hasta: Long): List<EstadoActividadEnFecha> {
        if (actividadIds.isEmpty()) return emptyList()
        return registroActividadDao.obtenerPorActividadIdsYRango(actividadIds, desde, hasta)
            .map { EstadoActividadEnFecha(it.actividadId, it.fecha, EstadoActividad.valueOf(it.estado)) }
    }

    override suspend fun obtenerTomasDeRango(actividadIds: List<String>, desde: Long, hasta: Long): List<TomaMedicamento> =
        actividadIds.flatMap { id -> tomaMedicamentoDao.obtenerPorActividadIdYRango(id, desde, hasta) }.map { it.toDomain() }

    override suspend fun obtenerEstadoToma(actividadId: String, fecha: Long, horario: String): EstadoActividad? =
        tomaMedicamentoDao.obtenerPorActividadIdYFecha(actividadId, fecha)
            .firstOrNull { it.horario == horario }
            ?.let { EstadoActividad.valueOf(it.estado) }

    override suspend fun guardarSesionesCita(sesiones: List<SesionCita>) {
        sesiones.forEach { sesionCitaDao.upsert(it.toEntity()) }
    }

    override suspend fun obtenerSesionesCita(actividadId: String): List<SesionCita> =
        sesionCitaDao.obtenerPorActividadId(actividadId).map { it.toDomain() }

    override suspend fun obtenerSesionesCitaDeRango(actividadIds: List<String>, desde: Long, hasta: Long): List<SesionCita> {
        if (actividadIds.isEmpty()) return emptyList()
        return sesionCitaDao.obtenerPorActividadIdsYRango(actividadIds, desde, hasta).map { it.toDomain() }
    }

    override suspend fun marcarSesionCita(actividadId: String, numeroSesion: Int, estado: EstadoActividad, usuarioId: String) {
        val existente = sesionCitaDao.obtenerPorNumeroSesion(actividadId, numeroSesion) ?: return
        sesionCitaDao.upsert(existente.copy(estado = estado.name))
        auditLogger.registrar(
            entidad = "sesion_cita",
            entidadId = existente.id,
            accion = AccionAuditoria.ACTUALIZAR,
            despues = existente.copy(estado = estado.name),
            usuarioId = usuarioId,
        )
    }

    override suspend fun reprogramarSesionCita(actividadId: String, numeroSesion: Int, nuevaFecha: Long, usuarioId: String) {
        val existente = sesionCitaDao.obtenerPorNumeroSesion(actividadId, numeroSesion) ?: return
        val actualizada = existente.copy(fecha = nuevaFecha)
        sesionCitaDao.upsert(actualizada)
        auditLogger.registrar(
            entidad = "sesion_cita",
            entidadId = existente.id,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = existente,
            despues = actualizada,
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerHistorialHabito(actividadId: String, dias: Int): List<DiaHistorialHabito> {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val fechaDesde = fechaHoy - (dias - 1)
        val registros = registroActividadDao
            .obtenerPorActividadIdYRango(actividadId, fechaDesde, fechaHoy)
            .associateBy { it.fecha }
        return (fechaDesde..fechaHoy).map { fecha ->
            val estado = registros[fecha]?.let { EstadoActividad.valueOf(it.estado) } ?: EstadoActividad.SIN_CONFIRMAR
            DiaHistorialHabito(fecha = fecha, estado = estado)
        }
    }

    private suspend fun resolverConDetalle(entidades: List<ActividadEntity>): List<Actividad> {
        val idsHabito = entidades.filter { it.tipo == TipoActividad.HABITO.name }.map { it.id }
        val idsTarea = entidades.filter { it.tipo == TipoActividad.TAREA.name }.map { it.id }
        val idsRutina = entidades.filter { it.tipo == TipoActividad.RUTINA.name }.map { it.id }
        val idsMedicamento = entidades.filter { it.tipo == TipoActividad.MEDICAMENTO.name }.map { it.id }
        val idsCita = entidades.filter { it.tipo == TipoActividad.CITA.name }.map { it.id }
        val idsFechaImportante = entidades.filter { it.tipo == TipoActividad.FECHA_IMPORTANTE.name }.map { it.id }

        val habitos = if (idsHabito.isNotEmpty()) {
            habitoDetalleDao.obtenerPorActividadIds(idsHabito).associateBy { it.actividadId }
        } else {
            emptyMap()
        }
        val tareas = if (idsTarea.isNotEmpty()) {
            tareaDetalleDao.obtenerPorActividadIds(idsTarea).associateBy { it.actividadId }
        } else {
            emptyMap()
        }
        val rutinas = idsRutina.associateWith { rutinaDetalleDao.obtenerPorActividadId(it) }
        val medicamentos = if (idsMedicamento.isNotEmpty()) {
            medicamentoDetalleDao.obtenerPorActividadIds(idsMedicamento).associateBy { it.actividadId }
        } else {
            emptyMap()
        }
        val citas = if (idsCita.isNotEmpty()) {
            citaDetalleDao.obtenerPorActividadIds(idsCita).associateBy { it.actividadId }
        } else {
            emptyMap()
        }
        val fechasImportantes = if (idsFechaImportante.isNotEmpty()) {
            fechaImportanteDetalleDao.obtenerPorActividadIds(idsFechaImportante).associateBy { it.actividadId }
        } else {
            emptyMap()
        }
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val registrosHoy = if (idsHabito.isNotEmpty()) {
            registroActividadDao.obtenerPorActividadIdsYFecha(idsHabito, fechaHoy).associateBy { it.actividadId }
        } else {
            emptyMap()
        }

        return entidades.map { entity ->
            val detalle = when (entity.tipo) {
                TipoActividad.HABITO.name -> habitos[entity.id]?.toDomain()
                TipoActividad.TAREA.name -> tareas[entity.id]?.toDomain()
                TipoActividad.RUTINA.name -> rutinas[entity.id]?.toDomain()
                TipoActividad.MEDICAMENTO.name -> medicamentos[entity.id]?.toDomain()
                TipoActividad.CITA.name -> citas[entity.id]?.toDomain()
                TipoActividad.FECHA_IMPORTANTE.name -> fechasImportantes[entity.id]?.toDomain()
                else -> null
            }
            val actividad = entity.toDomain(detalle)
            if (entity.tipo == TipoActividad.HABITO.name) {
                val estadoHoy = registrosHoy[entity.id]?.let { EstadoActividad.valueOf(it.estado) } ?: EstadoActividad.SIN_CONFIRMAR
                actividad.copy(estado = estadoHoy)
            } else {
                actividad
            }
        }
    }

    override suspend fun obtenerEliminadosDeRango(espacioId: String, desde: LocalDate, hasta: LocalDate): List<Pair<LocalDate, ItemAgenda>> {
        val desdeMillis = DateTimeUtils.localDateAEpochMillis(desde)
        val hastaMillis = DateTimeUtils.localDateAEpochMillis(hasta) + 86_400_000L - 1
        return historialCambiosDao
            .obtenerPorEntidadYAccionEnRango("actividad", AccionAuditoria.ELIMINAR.name, desdeMillis, hastaMillis)
            .mapNotNull { registro ->
                val json = registro.valoresAntesJson ?: return@mapNotNull null
                val entidadEliminada = runCatching { Json.decodeFromString<ActividadEntity>(json) }.getOrNull() ?: return@mapNotNull null
                if (entidadEliminada.espacioId != espacioId) return@mapNotNull null
                val tipo = runCatching { TipoActividad.valueOf(entidadEliminada.tipo) }.getOrNull() ?: return@mapNotNull null
                val fecha = DateTimeUtils.epochMillisToLocalDate(registro.timestamp)
                fecha to ItemAgenda(
                    actividadId = entidadEliminada.id,
                    tipo = tipo,
                    nombre = entidadEliminada.nombre,
                    horario = null,
                    momentoDelDia = null,
                    estado = EstadoActividad.OMITIDO,
                    subtitulo = "Eliminado",
                    eliminado = true,
                )
            }
    }
}
