package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.RegistroDiarioDao
import com.aqpseller.lulaapp.data.local.entity.RegistroDiarioEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RegistroDiarioRepositoryImpl @Inject constructor(
    private val registroDiarioDao: RegistroDiarioDao,
    private val auditLogger: AuditLogger,
) : RegistroDiarioRepository {

    override suspend fun cerrarDia(registro: RegistroDiario, usuarioId: String) {
        val entity = registro.toEntity()
        registroDiarioDao.upsert(entity)
        auditLogger.registrar<RegistroDiarioEntity>(
            entidad = "registro_diario",
            entidadId = registro.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun obtenerPorFecha(espacioId: String, fecha: Long): RegistroDiario? =
        registroDiarioDao.obtenerPorFecha(espacioId, fecha)?.toDomain()

    override fun observarHistorial(espacioId: String): Flow<List<RegistroDiario>> =
        registroDiarioDao.observarHistorial(espacioId).map { lista -> lista.map { it.toDomain() } }
}
