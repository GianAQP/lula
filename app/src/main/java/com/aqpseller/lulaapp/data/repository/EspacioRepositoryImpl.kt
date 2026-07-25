package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.AreaDeVidaDao
import com.aqpseller.lulaapp.data.local.dao.EspacioDao
import com.aqpseller.lulaapp.data.local.dao.EspacioMiembroDao
import com.aqpseller.lulaapp.data.local.entity.EspacioEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EspacioRepositoryImpl @Inject constructor(
    private val espacioDao: EspacioDao,
    private val espacioMiembroDao: EspacioMiembroDao,
    private val areaDeVidaDao: AreaDeVidaDao,
    private val auditLogger: AuditLogger,
) : EspacioRepository {

    override suspend fun contarEspacios(): Int = espacioDao.contar()

    override suspend fun crearEspacioPersonal(espacio: Espacio, miembro: EspacioMiembro) {
        val entity = espacio.toEntity()
        espacioDao.upsert(entity)
        espacioMiembroDao.upsert(miembro.toEntity())
        auditLogger.registrar<EspacioEntity>(
            entidad = "espacio",
            entidadId = espacio.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = espacio.creadoPor,
        )
    }

    override suspend fun obtenerEspacioPersonal(usuarioId: String): Espacio? =
        espacioDao.obtenerEspacioPersonal(usuarioId)?.toDomain()

    override suspend fun contarAreasDeVida(): Int = areaDeVidaDao.contar()

    override suspend fun sembrarAreasDeVida(areas: List<AreaDeVida>) {
        areaDeVidaDao.upsertTodas(areas.map { it.toEntity() })
    }

    override fun observarAreasDeVidaActivas(): Flow<List<AreaDeVida>> =
        areaDeVidaDao.observarActivas().map { lista -> lista.map { it.toDomain() } }
}
