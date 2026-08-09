package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.RegistroSemanalDao
import com.aqpseller.lulaapp.data.local.entity.RegistroSemanalEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.repository.RegistroSemanalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RegistroSemanalRepositoryImpl @Inject constructor(
    private val registroSemanalDao: RegistroSemanalDao,
    private val auditLogger: AuditLogger,
) : RegistroSemanalRepository {

    override suspend fun guardarRevision(registro: RegistroSemanal, usuarioId: String) {
        val entity = registro.toEntity()
        registroSemanalDao.upsert(entity)
        auditLogger.registrar<RegistroSemanalEntity>(
            entidad = "registro_semanal",
            entidadId = registro.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerPorSemana(espacioId: String, semana: String): RegistroSemanal? =
        registroSemanalDao.obtenerPorSemana(espacioId, semana)?.toDomain()

    override fun observarHistorial(espacioId: String): Flow<List<RegistroSemanal>> =
        registroSemanalDao.observarHistorial(espacioId).map { lista -> lista.map { it.toDomain() } }
}
