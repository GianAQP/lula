package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.HistorialCambiosDao
import com.aqpseller.lulaapp.data.local.entity.EspacioMiembroEntity
import com.aqpseller.lulaapp.domain.repository.EventoEliminacionMiembro
import com.aqpseller.lulaapp.domain.repository.HistorialCambiosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class HistorialCambiosRepositoryImpl @Inject constructor(
    private val historialCambiosDao: HistorialCambiosDao,
) : HistorialCambiosRepository {
    override fun observarEliminacionesDeMiembros(espacioId: String): Flow<List<EventoEliminacionMiembro>> =
        historialCambiosDao.observarEliminacionesDeMiembros("$espacioId:%").map { lista ->
            lista.mapNotNull { entity ->
                // El id compuesto es "espacioId:usuarioIdQuitado" (ver EspacioRepositoryImpl.eliminarMiembro).
                val objetivoUsuarioId = entity.entidadId.substringAfter(":", missingDelimiterValue = "")
                if (objetivoUsuarioId.isBlank()) return@mapNotNull null
                val objetivoNombre = entity.valoresAntesJson?.let {
                    runCatching { Json.decodeFromString<EspacioMiembroEntity>(it).nombre }.getOrNull()
                }
                EventoEliminacionMiembro(
                    timestamp = entity.timestamp,
                    actorUsuarioId = entity.usuarioId,
                    objetivoUsuarioId = objetivoUsuarioId,
                    objetivoNombre = objetivoNombre,
                )
            }
        }
}
