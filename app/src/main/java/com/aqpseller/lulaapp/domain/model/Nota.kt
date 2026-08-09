package com.aqpseller.lulaapp.domain.model

/** Nota de texto libre — si `titulo` es null, la lista usa la primera línea de `contenido`. */
data class Nota(
    val id: String,
    val espacioId: String,
    val propietario: String,
    val titulo: String?,
    val contenido: String,
    val fechaCreacion: Long,
    val fechaEdicion: Long,
    val orden: Int,
)
