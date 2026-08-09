package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.data.local.dao.ConexionDao
import com.aqpseller.lulaapp.data.local.entity.ConexionEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.Conexion
import com.aqpseller.lulaapp.domain.model.TipoConexion
import com.aqpseller.lulaapp.domain.repository.ConexionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConexionRepositoryImpl @Inject constructor(
    private val conexionDao: ConexionDao,
    private val auditLogger: AuditLogger,
) : ConexionRepository {

    override fun observarConexionesDe(usuarioId: String): Flow<List<Conexion>> =
        conexionDao.observarConexionesDe(usuarioId).map { lista -> lista.map { it.toDomain() } }

    override suspend fun crearSiNoExiste(usuarioA: String, usuarioB: String, tipo: TipoConexion, origenSolicitudId: String?) {
        if (conexionDao.obtenerEntre(usuarioA, usuarioB) != null) return
        val entity = ConexionEntity(
            id = IdGenerator.newId(),
            usuarioA = usuarioA,
            usuarioB = usuarioB,
            tipo = tipo.name,
            origenSolicitudId = origenSolicitudId,
            fechaConexion = DateTimeUtils.ahoraEpochMillis(),
        )
        conexionDao.upsert(entity)
        auditLogger.registrar<ConexionEntity>(
            entidad = "conexion",
            entidadId = entity.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioA,
        )
    }
}
