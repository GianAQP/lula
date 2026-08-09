package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.data.local.dao.PropositoPersonalDao
import com.aqpseller.lulaapp.data.local.entity.PropositoPersonalEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PropositoPersonalRepositoryImpl @Inject constructor(
    private val propositoPersonalDao: PropositoPersonalDao,
    private val auditLogger: AuditLogger,
) : PropositoPersonalRepository {

    override fun observar(espacioId: String): Flow<PropositoPersonal?> =
        propositoPersonalDao.observarPorEspacio(espacioId).map { it?.toDomain() }

    override suspend fun guardarRespuesta(espacioId: String, propietario: String, preguntaId: String, respuesta: String) {
        val actual = propositoPersonalDao.obtenerPorEspacio(espacioId)
        val respuestas = (actual?.toDomain()?.respuestas ?: emptyMap()) + (preguntaId to respuesta)
        val entity = PropositoPersonal(
            espacioId = espacioId,
            propietario = propietario,
            respuestas = respuestas,
            fechaEdicion = DateTimeUtils.ahoraEpochMillis(),
        ).toEntity()
        propositoPersonalDao.upsert(entity)
        auditLogger.registrar<PropositoPersonalEntity>(
            entidad = "proposito_personal",
            entidadId = espacioId,
            accion = if (actual == null) AccionAuditoria.CREAR else AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = entity,
            usuarioId = propietario,
        )
    }

    override suspend fun eliminarRespuesta(espacioId: String, propietario: String, preguntaId: String) {
        val actual = propositoPersonalDao.obtenerPorEspacio(espacioId) ?: return
        val respuestas = actual.toDomain().respuestas - preguntaId
        val entity = PropositoPersonal(
            espacioId = espacioId,
            propietario = propietario,
            respuestas = respuestas,
            fechaEdicion = DateTimeUtils.ahoraEpochMillis(),
        ).toEntity()
        propositoPersonalDao.upsert(entity)
        auditLogger.registrar<PropositoPersonalEntity>(
            entidad = "proposito_personal",
            entidadId = espacioId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = entity,
            usuarioId = propietario,
        )
    }

    override suspend fun eliminarTodo(espacioId: String, propietario: String) {
        val actual = propositoPersonalDao.obtenerPorEspacio(espacioId) ?: return
        propositoPersonalDao.eliminar(espacioId)
        auditLogger.registrar<PropositoPersonalEntity>(
            entidad = "proposito_personal",
            entidadId = espacioId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = propietario,
        )
    }
}
