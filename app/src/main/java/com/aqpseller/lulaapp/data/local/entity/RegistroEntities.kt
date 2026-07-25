package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "registro_diario",
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["espacioId", "fecha"], unique = true)],
)
data class RegistroDiarioEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val fecha: Long,
    val actividadesCompletadas: Int,
    val actividadesTotales: Int,
    val puntuacion: Int,
    val estadoAnimo: String?,
    val queLogre: String?,
    val queCosto: String?,
    val queAjusto: String?,
)

@Entity(
    tableName = "registro_semanal",
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["espacioId", "semana"], unique = true)],
)
data class RegistroSemanalEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val semana: String,
    val cumplimientoGeneralPorcentaje: Int,
    val rachaMaxima: Int,
    val queLogre: String?,
    val queNoFunciono: String?,
    val queAjusto: String?,
)
