package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Una fila por espacio Personal (nunca por Familia) — `respuestasJson` es un mapa
 * `id de pregunta → respuesta`, ver `domain.model.PREGUNTAS_PROPOSITO`. */
@Serializable
@Entity(
    tableName = "proposito_personal",
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class PropositoPersonalEntity(
    @PrimaryKey val espacioId: String,
    val propietario: String,
    val respuestasJson: String,
    val fechaEdicion: Long,
)
