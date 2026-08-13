package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
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
    /** Para POR_HABITO: días objetivo (ventana móvil). Para POR_MONTO/POR_NUMERO/MANUAL: cantidad objetivo. */
    val valorObjetivo: Double,
    /** Ignorado para POR_HABITO (se calcula en vivo desde `registro_actividad`). */
    val valorActual: Double,
    /** Último hito (0/25/50/75/100) ya celebrado en Hoy — evita repetir la misma tarjeta de
     * felicitación cada vez que se recompone. */
    val ultimoHitoCelebrado: Int = 0,
    val categoria: String? = null,
    /** Ya no se usa (reemplazado por `nivelRecordatorio`) — se deja la columna para no romper
     * filas ya guardadas por una versión anterior de la app; Room la sigue escribiendo con su
     * valor por defecto. */
    val avisarAlVencer: Boolean = false,
    val nivelRecordatorio: String = "SONIDO",
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
