package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.FinanzasDao
import com.aqpseller.lulaapp.data.local.entity.FinanzasEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
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

    override fun observarMovimientosEntrePeriodo(espacioId: String, desde: Long, hasta: Long): Flow<List<MovimientoFinanciero>> =
        finanzasDao.observarEntrePeriodo(espacioId, desde, hasta).map { lista -> lista.map { it.toDomain() } }
}
