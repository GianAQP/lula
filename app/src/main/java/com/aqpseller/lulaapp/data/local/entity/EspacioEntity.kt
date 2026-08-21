package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "espacio")
data class EspacioEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val nombre: String,
    val creadoPor: String,
    val fechaCreacion: Long,
)

@Serializable
@Entity(
    tableName = "espacio_miembro",
    primaryKeys = ["espacioId", "usuarioId"],
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("espacioId"), Index("usuarioId")],
)
data class EspacioMiembroEntity(
    val espacioId: String,
    val usuarioId: String,
    val rol: String,
)

@Entity(tableName = "area_de_vida")
data class AreaDeVidaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val activa: Boolean,
    val esPredefinida: Boolean,
)
