package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombreCompleto: String,
    val nombrePreferido: String,
    val correo: String?,
    val metodoLogin: String,
    val privacidadAceptadaEn: Long?,
    val modoDefectoAsistente: String?,
)
