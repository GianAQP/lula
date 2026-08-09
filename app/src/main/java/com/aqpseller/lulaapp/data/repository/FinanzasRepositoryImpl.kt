package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.FinanzasDao
import com.aqpseller.lulaapp.data.local.entity.FinanzasEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FinanzasRepositoryImpl @Inject constructor(
    private val finanzasDao: FinanzasDao,
    private val auditLogger: AuditLogger,
) : FinanzasRepository {

    override suspend fun registrarMovimiento(movimiento: MovimientoFinanciero, usuarioId: String) {
        val entity = movimiento.toEntity()
        finanzasDao.upsert(entity)
        auditLogger.registrar<FinanzasEntity>(
            entidad = "finanzas",
            entidadId = movimiento.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarMovimiento(
        movimientoId: String,
        tipo: TipoMovimientoFinanciero,
        monto: Double,
        categoria: String,
        descripcion: String?,
        fecha: Long,
        usuarioId: String,
    ) {
        val actual = finanzasDao.obtenerPorId(movimientoId) ?: return
        val actualizado = actual.copy(tipo = tipo.name, monto = monto, categoria = categoria, descripcion = descripcion, fecha = fecha)
        finanzasDao.upsert(actualizado)
        auditLogger.registrar<FinanzasEntity>(
            entidad = "finanzas",
            entidadId = movimientoId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun eliminarMovimiento(movimientoId: String, usuarioId: String) {
        val actual = finanzasDao.obtenerPorId(movimientoId) ?: return
        finanzasDao.eliminar(movimientoId)
        auditLogger.registrar<FinanzasEntity>(
            entidad = "finanzas",
            entidadId = movimientoId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerPorId(movimientoId: String): MovimientoFinanciero? =
        finanzasDao.obtenerPorId(movimientoId)?.toDomain()

    override fun observarMovimientosEntrePeriodo(espacioId: String, desde: Long, hasta: Long): Flow<List<MovimientoFinanciero>> =
        finanzasDao.observarEntrePeriodo(espacioId, desde, hasta).map { lista -> lista.map { it.toDomain() } }
}
