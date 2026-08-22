package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.data.local.dao.RetoFamiliarDao
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarParticipanteEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarRegistroEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ProgresoRetoFamiliar
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RetoFamiliarRepositoryImpl @Inject constructor(
    private val retoFamiliarDao: RetoFamiliarDao,
    private val auditLogger: AuditLogger,
) : RetoFamiliarRepository {

    override suspend fun crear(reto: RetoFamiliar, creadoPor: String) {
        val entity = reto.toEntity()
        retoFamiliarDao.upsert(entity)
        reto.participantesIds.forEach { usuarioId ->
            retoFamiliarDao.upsertParticipante(RetoFamiliarParticipanteEntity(retoId = reto.id, usuarioId = usuarioId))
        }
        auditLogger.registrar<RetoFamiliarEntity>(
            entidad = "reto_familiar",
            entidadId = reto.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = creadoPor,
        )
    }

    override fun observarConProgresoDeHoy(espacioId: String, usuarioId: String): Flow<List<ProgresoRetoFamiliar>> {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        return retoFamiliarDao.observarPorEspacio(espacioId).map { retos ->
            retos.map { retoEntity ->
                val participantes = retoFamiliarDao.obtenerParticipantes(retoEntity.id)
                val cumplidosHoy = retoFamiliarDao.obtenerRegistrosDeHoy(retoEntity.id, fechaHoy)
                    .filter { it.estado == EstadoActividad.CONFIRMADO.name }
                ProgresoRetoFamiliar(
                    reto = retoEntity.toDomain(participantes.map { it.usuarioId }),
                    cumplidosHoy = cumplidosHoy.size,
                    totalParticipantes = participantes.size,
                    yoCumpliHoy = cumplidosHoy.any { it.usuarioId == usuarioId },
                )
            }
        }
    }

    override suspend fun marcarCumplidoHoy(retoId: String, usuarioId: String, cumplido: Boolean) {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val existente = retoFamiliarDao.obtenerRegistro(retoId, usuarioId, fechaHoy)
        val registro = RetoFamiliarRegistroEntity(
            id = existente?.id ?: IdGenerator.newId(),
            retoId = retoId,
            usuarioId = usuarioId,
            fecha = fechaHoy,
            estado = if (cumplido) EstadoActividad.CONFIRMADO.name else EstadoActividad.SIN_CONFIRMAR.name,
        )
        retoFamiliarDao.upsertRegistro(registro)
        auditLogger.registrar<RetoFamiliarRegistroEntity>(
            entidad = "reto_familiar_registro",
            entidadId = registro.id,
            accion = if (existente == null) AccionAuditoria.CREAR else AccionAuditoria.ACTUALIZAR,
            antes = existente,
            despues = registro,
            usuarioId = usuarioId,
        )
    }

    override suspend fun mergeRemoto(reto: RetoFamiliar) {
        val entity = reto.toEntity()
        retoFamiliarDao.upsert(entity)
        reto.participantesIds.forEach { usuarioId ->
            retoFamiliarDao.upsertParticipante(RetoFamiliarParticipanteEntity(retoId = reto.id, usuarioId = usuarioId))
        }
    }

    override suspend fun mergeRegistroRemoto(retoId: String, usuarioId: String, fecha: Long, estado: EstadoActividad) {
        val existente = retoFamiliarDao.obtenerRegistro(retoId, usuarioId, fecha)
        retoFamiliarDao.upsertRegistro(
            RetoFamiliarRegistroEntity(
                id = existente?.id ?: IdGenerator.newId(),
                retoId = retoId,
                usuarioId = usuarioId,
                fecha = fecha,
                estado = estado.name,
            ),
        )
    }
}
