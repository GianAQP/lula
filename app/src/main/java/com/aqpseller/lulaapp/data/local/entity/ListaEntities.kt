package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "lista",
    foreignKeys = [
        ForeignKey(
            entity = EspacioEntity::class,
            parentColumns = ["id"],
            childColumns = ["espacioId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("espacioId")],
)
data class ListaEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val nombre: String,
    val fechaCreacion: Long,
    val orden: Int = 0,
)

@Serializable
@Entity(
    tableName = "lista_item",
    foreignKeys = [
        ForeignKey(
            entity = ListaEntity::class,
            parentColumns = ["id"],
            childColumns = ["listaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listaId")],
)
data class ListaItemEntity(
    @PrimaryKey val id: String,
    val listaId: String,
    val texto: String,
    val marcado: Boolean,
    val orden: Int,
)

@Serializable
@Entity(
    tableName = "lista_ejecucion",
    foreignKeys = [
        ForeignKey(
            entity = ListaEntity::class,
            parentColumns = ["id"],
            childColumns = ["listaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listaId")],
)
data class ListaEjecucionEntity(
    @PrimaryKey val id: String,
    val listaId: String,
    val fecha: Long,
    val itemsJson: String,
)
