package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "finanzas",
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("espacioId"), Index("fecha"), Index("tipo")],
)
data class FinanzasEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val tipo: String,
    val monto: Double,
    val categoria: String,
    val descripcion: String?,
    val fecha: Long,
    val privacidad: String,
)
