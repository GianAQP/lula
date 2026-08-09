package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "conexion",
    indices = [Index("usuarioA"), Index("usuarioB")],
)
data class ConexionEntity(
    @PrimaryKey val id: String,
    val usuarioA: String,
    val usuarioB: String,
    val tipo: String,
    val origenSolicitudId: String?,
    val fechaConexion: Long,
)
