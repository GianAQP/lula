package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.ActividadDao
import com.aqpseller.lulaapp.data.local.dao.HabitoDetalleDao
import com.aqpseller.lulaapp.data.local.dao.TareaDetalleDao
import com.aqpseller.lulaapp.data.local.entity.ActividadEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ActividadRepositoryImpl @Inject constructor(
    private val actividadDao: ActividadDao,
    private val habitoDetalleDao: HabitoDetalleDao,
    private val tareaDetalleDao: TareaDetalleDao,
    private val auditLogger: AuditLogger,
) : ActividadRepository {

    override suspend fun crearHabito(actividad: Actividad, detalle: ActividadDetalle.Habito, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        habitoDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun crearTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea, usuarioId: String) {
        val entity = actividad.toEntity()
        actividadDao.upsert(entity)
        tareaDetalleDao.upsert(detalle.toEntity(actividad.id))
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = actividad.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun marcarEstado(id: String, estado: EstadoActividad, usuarioId: String) {
        val antes = actividadDao.obtenerPorId(id)
        actividadDao.actualizarEstado(id, estado.name)
        val despues = actividadDao.obtenerPorId(id)
        auditLogger.registrar<ActividadEntity>(
            entidad = "actividad",
            entidadId = id,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = antes,
            despues = despues,
            usuarioId = usuarioId,
        )
    }

    override fun observarActividadesDeEspacio(espacioId: String): Flow<List<Actividad>> =
        actividadDao.observarActivasDeEspacio(espacioId).map { entidades ->
            val idsHabito = entidades.filter { it.tipo == TipoActividad.HABITO.name }.map { it.id }
            val idsTarea = entidades.filter { it.tipo == TipoActividad.TAREA.name }.map { it.id }

            val habitos = if (idsHabito.isNotEmpty()) {
                habitoDetalleDao.obtenerPorActividadIds(idsHabito).associateBy { it.actividadId }
            } else {
                emptyMap()
            }
            val tareas = if (idsTarea.isNotEmpty()) {
                tareaDetalleDao.obtenerPorActividadIds(idsTarea).associateBy { it.actividadId }
            } else {
                emptyMap()
            }

            entidades.map { entity ->
                val detalle = when (entity.tipo) {
                    TipoActividad.HABITO.name -> habitos[entity.id]?.toDomain()
                    TipoActividad.TAREA.name -> tareas[entity.id]?.toDomain()
                    else -> null
                }
                entity.toDomain(detalle)
            }
        }
}
