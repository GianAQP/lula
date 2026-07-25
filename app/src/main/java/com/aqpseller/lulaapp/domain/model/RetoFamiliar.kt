package com.aqpseller.lulaapp.domain.model

data class RetoFamiliar(
    val id: String,
    val espacioId: String,
    val nombre: String,
    val objetivo: String,
    val frecuencia: FrecuenciaHabito,
    val participantesIds: List<String>,
    val recompensa: String?,
)

data class SolicitudCompartir(
    val id: String,
    val de: String,
    val para: String,
    val tieneCuenta: Boolean,
    val elementoId: String,
    val contexto: String,
    val permisos: String,
    val estado: String,
    val canalEnvio: String?,
    val fechaSolicitud: Long,
    val fechaRespuesta: Long? = null,
)
