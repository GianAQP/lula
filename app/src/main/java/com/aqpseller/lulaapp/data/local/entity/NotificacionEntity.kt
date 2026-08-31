package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "notificacion",
    indices = [Index("fecha"), Index("leido")],
)
data class NotificacionEntity(
    @PrimaryKey val id: String,
    val emoji: String,
    val titulo: String,
    val cuerpo: String,
    val fecha: Long,
    val leido: Boolean = false,
    /** Si no es null, tocar la fila puede llevar a "Mi círculo de cuidado" a aceptar/rechazar
     * (solo si esa solicitud sigue PENDIENTE — ver `NotificacionesViewModel`). */
    val solicitudId: String? = null,
)
