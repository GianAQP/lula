package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.data.local.dao.ListaDao
import com.aqpseller.lulaapp.data.local.dao.ListaEjecucionDao
import com.aqpseller.lulaapp.data.local.dao.ListaItemDao
import com.aqpseller.lulaapp.data.local.entity.ListaEntity
import com.aqpseller.lulaapp.data.local.entity.ListaItemEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.ItemListaSnapshot
import com.aqpseller.lulaapp.domain.model.ListaConItems
import com.aqpseller.lulaapp.domain.model.ListaEjecucion
import com.aqpseller.lulaapp.domain.model.ListaResumen
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ListaRepositoryImpl @Inject constructor(
    private val listaDao: ListaDao,
    private val listaItemDao: ListaItemDao,
    private val listaEjecucionDao: ListaEjecucionDao,
    private val auditLogger: AuditLogger,
) : ListaRepository {

    override suspend fun crear(espacioId: String, nombre: String, itemsTexto: List<String>, usuarioId: String): String {
        val listaId = IdGenerator.newId()
        val entity = ListaEntity(id = listaId, espacioId = espacioId, nombre = nombre, fechaCreacion = DateTimeUtils.ahoraEpochMillis())
        listaDao.upsert(entity)
        itemsTexto.forEachIndexed { index, texto ->
            listaItemDao.upsert(ListaItemEntity(id = IdGenerator.newId(), listaId = listaId, texto = texto, marcado = false, orden = index))
        }
        auditLogger.registrar<ListaEntity>(
            entidad = "lista",
            entidadId = listaId,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
        return listaId
    }

    override fun observarResumenPorEspacio(espacioId: String): Flow<List<ListaResumen>> =
        listaDao.observarConConteo(espacioId).map { filas ->
            filas.map { ListaResumen(id = it.id, nombre = it.nombre, total = it.total, marcados = it.marcados) }
        }

    override fun observarConItems(listaId: String): Flow<ListaConItems?> =
        combine(listaDao.observarPorId(listaId), listaItemDao.observarPorLista(listaId)) { lista, items ->
            lista?.let { ListaConItems(id = it.id, nombre = it.nombre, items = items.map { item -> item.toDomain() }) }
        }

    override suspend fun agregarItem(listaId: String, texto: String, usuarioId: String) {
        val siguienteOrden = (listaItemDao.obtenerPorLista(listaId).maxOfOrNull { it.orden } ?: -1) + 1
        val entity = ListaItemEntity(id = IdGenerator.newId(), listaId = listaId, texto = texto, marcado = false, orden = siguienteOrden)
        listaItemDao.upsert(entity)
        auditLogger.registrar<ListaItemEntity>(
            entidad = "lista_item",
            entidadId = entity.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuarioId,
        )
    }

    override suspend fun marcarItem(itemId: String, marcado: Boolean, usuarioId: String) {
        val actual = listaItemDao.obtenerPorId(itemId) ?: return
        val actualizado = actual.copy(marcado = marcado)
        listaItemDao.upsert(actualizado)
        auditLogger.registrar<ListaItemEntity>(
            entidad = "lista_item",
            entidadId = itemId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun eliminarItem(itemId: String, usuarioId: String) {
        val actual = listaItemDao.obtenerPorId(itemId) ?: return
        listaItemDao.eliminar(itemId)
        auditLogger.registrar<ListaItemEntity>(
            entidad = "lista_item",
            entidadId = itemId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }

    override suspend fun reiniciar(listaId: String, usuarioId: String) {
        val itemsActuales = listaItemDao.obtenerPorLista(listaId)
        if (itemsActuales.isNotEmpty()) {
            val ejecucion = ListaEjecucion(
                id = IdGenerator.newId(),
                listaId = listaId,
                fecha = DateTimeUtils.ahoraEpochMillis(),
                items = itemsActuales.map { ItemListaSnapshot(texto = it.texto, marcado = it.marcado) },
            )
            listaEjecucionDao.upsert(ejecucion.toEntity())
        }
        listaItemDao.desmarcarTodos(listaId)
        auditLogger.registrar<ListaEntity>(
            entidad = "lista",
            entidadId = listaId,
            accion = AccionAuditoria.ACTUALIZAR,
            usuarioId = usuarioId,
        )
    }

    override fun observarHistorial(listaId: String): Flow<List<ListaEjecucion>> =
        listaEjecucionDao.observarPorLista(listaId).map { ejecuciones -> ejecuciones.map { it.toDomain() } }

    override suspend fun eliminarLista(listaId: String, usuarioId: String) {
        val actual = listaDao.obtenerPorId(listaId) ?: return
        listaDao.eliminar(listaId)
        auditLogger.registrar<ListaEntity>(
            entidad = "lista",
            entidadId = listaId,
            accion = AccionAuditoria.ELIMINAR,
            antes = actual,
            usuarioId = usuarioId,
        )
    }
}
