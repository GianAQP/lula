package com.aqpseller.lulaapp.data.repository

import androidx.room.withTransaction
import com.aqpseller.lulaapp.core.database.LulaDatabase
import com.aqpseller.lulaapp.data.local.dao.ActividadDao
import com.aqpseller.lulaapp.data.local.dao.AreaDeVidaDao
import com.aqpseller.lulaapp.data.local.dao.EspacioDao
import com.aqpseller.lulaapp.data.local.dao.EspacioMiembroDao
import com.aqpseller.lulaapp.data.local.dao.FinanzasDao
import com.aqpseller.lulaapp.data.local.dao.ListaDao
import com.aqpseller.lulaapp.data.local.dao.MetaDao
import com.aqpseller.lulaapp.data.local.entity.EspacioEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioMiembroEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EspacioRepositoryImpl @Inject constructor(
    private val espacioDao: EspacioDao,
    private val espacioMiembroDao: EspacioMiembroDao,
    private val areaDeVidaDao: AreaDeVidaDao,
    private val actividadDao: ActividadDao,
    private val metaDao: MetaDao,
    private val listaDao: ListaDao,
    private val finanzasDao: FinanzasDao,
    private val lulaDatabase: LulaDatabase,
    private val auditLogger: AuditLogger,
) : EspacioRepository {

    override suspend fun contarEspacios(): Int = espacioDao.contar()

    override suspend fun crearEspacioPersonal(espacio: Espacio, miembro: EspacioMiembro) =
        crearEspacio(espacio, miembro)

    override suspend fun crearEspacio(espacio: Espacio, miembro: EspacioMiembro) {
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

    override fun observarEspaciosDeUsuario(usuarioId: String): Flow<List<Espacio>> =
        espacioDao.observarEspaciosDeUsuario(usuarioId).map { lista -> lista.map { it.toDomain() } }

    override suspend fun obtenerEspacioSiEsMiembro(espacioId: String, usuarioId: String): Espacio? {
        espacioMiembroDao.obtenerMiembro(espacioId, usuarioId) ?: return null
        return espacioDao.obtenerPorId(espacioId)?.toDomain()
    }

    override fun observarMiembros(espacioId: String): Flow<List<EspacioMiembro>> =
        espacioMiembroDao.observarMiembros(espacioId).map { lista -> lista.map { it.toDomain() } }

    override suspend fun asegurarEspacioMinimo(espacioId: String, nombre: String, creadoPor: String, tipo: TipoEspacio) {
        if (espacioDao.obtenerPorId(espacioId) != null) return
        val entity = EspacioEntity(id = espacioId, tipo = tipo.name, nombre = nombre, creadoPor = creadoPor, fechaCreacion = DateTimeUtils.ahoraEpochMillis())
        espacioDao.upsert(entity)
        auditLogger.registrar<EspacioEntity>(
            entidad = "espacio",
            entidadId = espacioId,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = creadoPor,
        )
    }

    override suspend fun agregarMiembro(espacioId: String, usuarioId: String, rol: RolEnEspacio, nombre: String?) {
        val existente = espacioMiembroDao.obtenerMiembro(espacioId, usuarioId)
        val entity = EspacioMiembro(espacioId = espacioId, usuarioId = usuarioId, rol = rol, nombre = nombre ?: existente?.nombre).toEntity()
        espacioMiembroDao.upsert(entity)
        auditLogger.registrar<EspacioMiembroEntity>(
            entidad = "espacio_miembro",
            entidadId = "$espacioId:$usuarioId",
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun renombrarEspacio(espacioId: String, nuevoNombre: String, usuarioId: String) {
        val actual = espacioDao.obtenerPorId(espacioId) ?: return
        espacioDao.actualizarNombre(espacioId, nuevoNombre)
        auditLogger.registrar<EspacioEntity>(
            entidad = "espacio",
            entidadId = espacioId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actual.copy(nombre = nuevoNombre),
            usuarioId = usuarioId,
        )
    }

    /**
     * `actividad`/`meta`/`lista`/`finanzas` tienen FK `RESTRICT` hacia `espacio` a propósito
     * (evita borrar un espacio con datos por accidente en el resto de la app) — acá, donde sí
     * es intencional, se limpian primero en una transacción. `espacio_miembro` y
     * `reto_familiar` (+ sus tablas hijas) sí cascadean solos al borrar la fila de `espacio`.
     * Notas/Diario nunca llegan a tener filas en un espacio Familia (siempre usan
     * `espacioPersonalId`, ver `SesionActual`), así que no hace falta limpiarlas acá.
     */
    override suspend fun eliminarEspacio(espacioId: String, usuarioId: String) {
        val actual = espacioDao.obtenerPorId(espacioId) ?: return
        lulaDatabase.withTransaction {
            actividadDao.eliminarPorEspacio(espacioId)
            metaDao.eliminarPorEspacio(espacioId)
            listaDao.eliminarPorEspacio(espacioId)
            finanzasDao.eliminarPorEspacio(espacioId)
            espacioDao.eliminar(espacioId)
        }
        auditLogger.registrar<EspacioEntity>(
            entidad = "espacio",
            entidadId = espacioId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun contarAreasDeVida(): Int = areaDeVidaDao.contar()

    override suspend fun sembrarAreasDeVida(areas: List<AreaDeVida>) {
        areaDeVidaDao.upsertTodas(areas.map { it.toEntity() })
    }

    override fun observarAreasDeVidaActivas(): Flow<List<AreaDeVida>> =
        areaDeVidaDao.observarActivas().map { lista -> lista.map { it.toDomain() } }
}
