package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.data.local.dao.SolicitudCompartirDao
import com.aqpseller.lulaapp.data.local.entity.SolicitudCompartirEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SolicitudCompartirRepositoryImpl @Inject constructor(
    private val solicitudCompartirDao: SolicitudCompartirDao,
    private val auditLogger: AuditLogger,
) : SolicitudCompartirRepository {

    override suspend fun crear(solicitud: SolicitudCompartir, usuarioId: String) {
        val entity = solicitud.toEntity()
        solicitudCompartirDao.upsert(entity)
        auditLogger.registrar<SolicitudCompartirEntity>(
            entidad = "solicitud_compartir",
            entidadId = solicitud.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun cancelar(solicitudId: String, usuarioId: String) {
        val actual = solicitudCompartirDao.obtenerPorId(solicitudId) ?: return
        solicitudCompartirDao.eliminar(solicitudId)
        auditLogger.registrar<SolicitudCompartirEntity>(
            entidad = "solicitud_compartir",
            entidadId = solicitudId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun responder(solicitudId: String, estado: EstadoSolicitud, usuarioId: String) {
        val actual = solicitudCompartirDao.obtenerPorId(solicitudId) ?: return
        val actualizada = actual.copy(estado = estado.name, fechaRespuesta = DateTimeUtils.ahoraEpochMillis())
        solicitudCompartirDao.upsert(actualizada)
        auditLogger.registrar<SolicitudCompartirEntity>(
            entidad = "solicitud_compartir",
            entidadId = solicitudId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizada,
            usuarioId = usuarioId,
        )
    }

    override fun observarEnviadasPor(usuarioId: String): Flow<List<SolicitudCompartir>> =
        solicitudCompartirDao.observarEnviadasPor(usuarioId).map { lista -> lista.map { it.toDomain() } }

    override suspend fun obtenerPorId(solicitudId: String): SolicitudCompartir? =
        solicitudCompartirDao.obtenerPorId(solicitudId)?.toDomain()

    override fun observarPendientesPara(correo: String): Flow<List<SolicitudCompartir>> =
        solicitudCompartirDao.observarPendientesPara(correo).map { lista -> lista.map { it.toDomain() } }
}
