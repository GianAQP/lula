package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "solicitud_compartir",
    indices = [Index("de"), Index("para"), Index("estado")],
)
data class SolicitudCompartirEntity(
    @PrimaryKey val id: String,
    val de: String,
    val para: String,
    val tieneCuenta: Boolean,
    val elementoId: String,
    val contexto: String,
    val permisos: String,
    val estado: String,
    val canalEnvio: String?,
    val fechaSolicitud: Long,
    val fechaRespuesta: Long?,
)
