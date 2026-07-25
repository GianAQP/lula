package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.data.local.dao.HistorialCambiosDao
import com.aqpseller.lulaapp.data.local.entity.HistorialCambiosEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.OrigenCambio
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Registra `HistorialCambios` para cada escritura. Se inyecta en cada *RepositoryImpl* y se
 * llama dentro del mismo método que escribe en Room — nunca en un caso de uso aparte, para
 * que sea imposible de omitir al agregar un flujo nuevo (lección de auditoría de Mayia,
 * ver `Plan/08-decisiones-tecnicas.md`).
 */
class AuditLogger @Inject constructor(
    @PublishedApi internal val historialCambiosDao: HistorialCambiosDao,
) {
    suspend inline fun <reified T> registrar(
        entidad: String,
        entidadId: String,
        accion: AccionAuditoria,
        antes: T? = null,
        despues: T? = null,
        usuarioId: String,
    ) {
        historialCambiosDao.upsert(
            HistorialCambiosEntity(
                id = IdGenerator.newId(),
                entidad = entidad,
                entidadId = entidadId,
                accion = accion.name,
                valoresAntesJson = antes?.let { Json.encodeToString(it) },
                valoresDespuesJson = despues?.let { Json.encodeToString(it) },
                usuarioId = usuarioId,
                timestamp = DateTimeUtils.ahoraEpochMillis(),
                origen = OrigenCambio.LOCAL.name,
            ),
        )
    }
}
