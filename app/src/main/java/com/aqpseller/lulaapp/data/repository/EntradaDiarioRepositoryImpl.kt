package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.EntradaDiarioDao
import com.aqpseller.lulaapp.data.local.entity.EntradaDiarioEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EntradaDiarioRepositoryImpl @Inject constructor(
    private val entradaDiarioDao: EntradaDiarioDao,
    private val auditLogger: AuditLogger,
) : EntradaDiarioRepository {

    override suspend fun crear(entrada: EntradaDiario) {
        val entity = entrada.toEntity()
        entradaDiarioDao.upsert(entity)
        auditLogger.registrar<EntradaDiarioEntity>(
            entidad = "entrada_diario",
            entidadId = entrada.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = entrada.propietario,
        )
    }

    override suspend fun actualizar(entrada: EntradaDiario, usuarioId: String) {
        val actual = entradaDiarioDao.obtenerPorId(entrada.id)
        val entity = entrada.toEntity()
        entradaDiarioDao.upsert(entity)
        auditLogger.registrar<EntradaDiarioEntity>(
            entidad = "entrada_diario",
            entidadId = entrada.id,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun eliminar(entradaId: String, usuarioId: String) {
        val actual = entradaDiarioDao.obtenerPorId(entradaId) ?: return
        entradaDiarioDao.eliminar(entradaId)
        auditLogger.registrar<EntradaDiarioEntity>(
            entidad = "entrada_diario",
            entidadId = entradaId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerPorId(entradaId: String): EntradaDiario? = entradaDiarioDao.obtenerPorId(entradaId)?.toDomain()

    override fun observarPorEspacio(espacioId: String): Flow<List<EntradaDiario>> =
        entradaDiarioDao.observarPorEspacio(espacioId).map { lista -> lista.map { it.toDomain() } }

    override fun buscar(espacioId: String, consulta: String): Flow<List<EntradaDiario>> =
        entradaDiarioDao.buscar(espacioId, consulta).map { lista -> lista.map { it.toDomain() } }
}
