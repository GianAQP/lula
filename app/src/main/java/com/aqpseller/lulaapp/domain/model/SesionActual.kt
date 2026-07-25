package com.aqpseller.lulaapp.domain.model

/** Usuario + espacio personal resueltos, usados por todos los ViewModels de features. */
data class SesionActual(
    val usuarioId: String,
    val espacioId: String,
)
