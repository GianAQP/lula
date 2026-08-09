package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "entrada_diario",
    indices = [Index("espacioId"), Index("fecha")],
)
data class EntradaDiarioEntity(
    @PrimaryKey val id: String,
    val espacioId: String,
    val propietario: String,
    val titulo: String?,
    val texto: String,
    val areaDeVidaId: String?,
    val fecha: Long,
    val privacidad: String,
    val fotosJson: String?,
)
