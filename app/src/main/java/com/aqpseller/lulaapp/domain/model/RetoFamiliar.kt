package com.aqpseller.lulaapp.domain.model

data class RetoFamiliar(
    val id: String,
    val espacioId: String,
    val nombre: String,
    val objetivo: String,
    val frecuencia: FrecuenciaRetoFamiliar,
    val participantesIds: List<String>,
    /** Texto libre, la define la familia — ver `Plan/02-pantallas.md`. */
    val recompensa: String?,
)

/** Un Reto familiar con cuántos de sus participantes ya cumplieron hoy — para su pantalla. */
data class ProgresoRetoFamiliar(
    val reto: RetoFamiliar,
    val cumplidosHoy: Int,
    val totalParticipantes: Int,
    val yoCumpliHoy: Boolean,
)
