package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "nota",
    indices = [Index("espacioId"), Index("fechaEdicion")],
)
data class NotaEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val propietario: String,
    val titulo: String?,
    val contenido: String,
    val fechaCreacion: Long,
    val fechaEdicion: Long,
    /** Orden manual del usuario (flechas ▲▼) — menor va primero. Nunca se recalcula sola. */
    val orden: Int,
)
