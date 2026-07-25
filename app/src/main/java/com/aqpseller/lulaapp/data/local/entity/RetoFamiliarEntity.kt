package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reto_familiar",
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("espacioId")],
)
data class RetoFamiliarEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val nombre: String,
    val objetivo: String,
    val frecuencia: String,
    val recompensa: String?,
)

@Entity(
    tableName = "reto_familiar_participante",
    primaryKeys = ["retoId", "usuarioId"],
    foreignKeys = [
        ForeignKey(
            entity = RetoFamiliarEntity::class,
            parentColumns = ["id"],
            childColumns = ["retoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("retoId")],
)
data class RetoFamiliarParticipanteEntity(
    val retoId: String,
    val usuarioId: String,
)
