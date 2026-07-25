package com.aqpseller.lulaapp.domain.model

data class Espacio(
    val id: String,
    val tipo: TipoEspacio,
    val nombre: String,
    val creadoPor: String,
    val fechaCreacion: Long,
)

data class EspacioMiembro(
    val espacioId: String,
    val usuarioId: String,
    val rol: RolEnEspacio,
)

data class AreaDeVida(
    val id: String,
    val nombre: String,
    val activa: Boolean = true,
    val esPredefinida: Boolean = true,
)
