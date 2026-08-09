package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.NotaDao
import com.aqpseller.lulaapp.data.local.entity.NotaEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotaRepositoryImpl @Inject constructor(
    private val notaDao: NotaDao,
    private val auditLogger: AuditLogger,
) : NotaRepository {

    override suspend fun crear(nota: Nota) {
        val entity = nota.toEntity()
        notaDao.upsert(entity)
        auditLogger.registrar<NotaEntity>(
            entidad = "nota",
            entidadId = nota.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = nota.propietario,
        )
    }

    override suspend fun actualizar(nota: Nota, usuarioId: String) {
        val actual = notaDao.obtenerPorId(nota.id)
        val entity = nota.toEntity()
        notaDao.upsert(entity)
        auditLogger.registrar<NotaEntity>(
            entidad = "nota",
            entidadId = nota.id,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun eliminar(notaId: String, usuarioId: String) {
        val actual = notaDao.obtenerPorId(notaId) ?: return
        notaDao.eliminar(notaId)
        auditLogger.registrar<NotaEntity>(
            entidad = "nota",
            entidadId = notaId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerPorId(notaId: String): Nota? = notaDao.obtenerPorId(notaId)?.toDomain()

    override fun observarPorEspacio(espacioId: String): Flow<List<Nota>> =
        notaDao.observarPorEspacio(espacioId).map { lista -> lista.map { it.toDomain() } }

    override suspend fun obtenerOrdenParaNota(espacioId: String): Int =
        (notaDao.obtenerOrdenMinimo(espacioId) ?: 1) - 1

    override suspend fun actualizarOrden(notaId: String, nuevoOrden: Int, usuarioId: String) {
        val actual = notaDao.obtenerPorId(notaId) ?: return
        notaDao.actualizarOrden(notaId, nuevoOrden)
        auditLogger.registrar<NotaEntity>(
            entidad = "nota",
            entidadId = notaId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actual.copy(orden = nuevoOrden),
            usuarioId = usuarioId,
        )
    }
}
