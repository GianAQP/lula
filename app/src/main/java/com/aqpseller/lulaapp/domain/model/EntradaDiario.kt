package com.aqpseller.lulaapp.domain.model

data class EntradaDiario(
    val id: String,
    val espacioId: String,
    val propietario: String,
    val titulo: String?,
    val texto: String,
    val areaDeVidaId: String?,
    val fecha: Long,
    val privacidad: Privacidad,
    val fotos: List<String> = emptyList(),
)
