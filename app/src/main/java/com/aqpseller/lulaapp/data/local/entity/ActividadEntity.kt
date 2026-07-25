package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Tabla base de la entidad genérica `Actividad` (ver `Plan/01-arquitectura.md` y
 * `Plan/08-decisiones-tecnicas.md`). Los campos específicos por tipo viven en tablas de
 * detalle 1:1 (`HabitoDetalleEntity`, `TareaDetalleEntity`, etc.), nunca en columnas nullable
 * aquí. `espacioId`, `tipo`, `fechaCreacion`, `estado`, `momentoDelDia`, `areaDeVidaId` y
 * `propietario` están indexados porque son los campos por los que Hoy y Progreso filtran.
 */
@Entity(
    tableName = "actividad",
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AreaDeVidaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaDeVidaId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("espacioId"),
        Index("tipo"),
        Index("fechaCreacion"),
        Index("estado"),
        Index("momentoDelDia"),
        Index("areaDeVidaId"),
        Index("propietario"),
    ],
)
@Serializable
data class ActividadEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val espacioId: String,
    val nombre: String,
    val propietario: String,
    val responsablesJson: String,
    val puedeVerJson: String,
    val puedeRecordarJson: String,
    val estado: String,
    val privacidad: String,
    val syncStatus: String,
    val esPremiumFeature: Boolean,
    val areaDeVidaId: String?,
    val momentoDelDia: String?,
    val fechaCreacion: Long,
)
