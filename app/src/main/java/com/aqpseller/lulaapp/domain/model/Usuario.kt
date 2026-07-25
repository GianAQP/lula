package com.aqpseller.lulaapp.domain.model

data class Usuario(
    val id: String,
    val nombreCompleto: String,
    val nombrePreferido: String,
    val correo: String?,
    val metodoLogin: MetodoLogin,
    val privacidadAceptadaEn: Long?,
    val modoDefectoAsistente: String? = null,
)
