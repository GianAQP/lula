package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.MetaDao
import com.aqpseller.lulaapp.data.local.entity.MetaActividadCrossRef
import com.aqpseller.lulaapp.data.local.entity.MetaEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MetaRepositoryImpl @Inject constructor(
    private val metaDao: MetaDao,
    private val auditLogger: AuditLogger,
) : MetaRepository {

    override suspend fun crear(meta: Meta, usuarioId: String) {
        val entity = meta.toEntity()
        metaDao.upsert(entity)
        meta.actividadesVinculadasIds.forEach { actividadId ->
            metaDao.upsertVinculo(MetaActividadCrossRef(metaId = meta.id, actividadId = actividadId))
        }
        auditLogger.registrar<MetaEntity>(
            entidad = "meta",
            entidadId = meta.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizar(meta: Meta, usuarioId: String) {
        val actual = metaDao.obtenerPorId(meta.id) ?: return
        val entity = meta.toEntity()
        metaDao.upsert(entity)
        meta.actividadesVinculadasIds.forEach { actividadId ->
            metaDao.upsertVinculo(MetaActividadCrossRef(metaId = meta.id, actividadId = actividadId))
        }
        auditLogger.registrar<MetaEntity>(
            entidad = "meta",
            entidadId = meta.id,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun eliminar(metaId: String, usuarioId: String) {
        val actual = metaDao.obtenerPorId(metaId) ?: return
        metaDao.eliminar(metaId)
        auditLogger.registrar<MetaEntity>(
            entidad = "meta",
            entidadId = metaId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun agregarProgreso(metaId: String, incremento: Double, usuarioId: String) {
        val actual = metaDao.obtenerPorId(metaId) ?: return
        val nuevoValor = actual.valorActual + incremento
        metaDao.actualizarValorActual(metaId, nuevoValor)
        auditLogger.registrar<MetaEntity>(
            entidad = "meta",
            entidadId = metaId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actual.copy(valorActual = nuevoValor),
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerConVinculo(metaId: String): Meta? {
        val entity = metaDao.obtenerPorId(metaId) ?: return null
        val vinculo = metaDao.obtenerActividadVinculada(metaId)
        return entity.toDomain(listOfNotNull(vinculo))
    }

    override fun observarPorEspacio(espacioId: String): Flow<List<Meta>> =
        metaDao.observarPorEspacio(espacioId).map { entidades ->
            entidades.map { entity ->
                val vinculo = metaDao.obtenerActividadVinculada(entity.id)
                entity.toDomain(listOfNotNull(vinculo))
            }
        }
}
