package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meta",
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
    indices = [Index("espacioId"), Index("areaDeVidaId")],
)
data class MetaEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val nombre: String,
    val areaDeVidaId: String?,
    val fechaLimite: Long?,
    val comoSeMide: String,
)

@Entity(
    tableName = "meta_actividad_cross_ref",
    primaryKeys = ["metaId", "actividadId"],
    foreignKeys = [
        ForeignKey(
            entity = MetaEntity::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("metaId"), Index("actividadId")],
)
data class MetaActividadCrossRef(
    val metaId: String,
    val actividadId: String,
)
