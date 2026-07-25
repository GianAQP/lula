package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Auditoría desde el MVP (ver `Plan/08-decisiones-tecnicas.md`): se escribe dentro de cada
 * método de escritura de los repositorios, nunca en un caso de uso aparte.
 */
@Entity(
    tableName = "historial_cambios",
    indices = [Index(value = ["entidad", "entidadId"]), Index("timestamp")],
)
data class HistorialCambiosEntity(
    @PrimaryKey val id: String,
    val entidad: String,
    val entidadId: String,
    val accion: String,
    val valoresAntesJson: String?,
    val valoresDespuesJson: String?,
    val usuarioId: String,
    val timestamp: Long,
    val origen: String,
)
