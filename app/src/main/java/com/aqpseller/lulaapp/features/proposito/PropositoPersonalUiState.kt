package com.aqpseller.lulaapp.features.proposito

import com.aqpseller.lulaapp.domain.model.SeccionProposito

data class PreguntaPropositoUi(
    val id: String,
    val texto: String,
    val seccion: SeccionProposito,
    val respuesta: String?,
)

data class PropositoPersonalUiState(
    val cargando: Boolean = true,
    val preguntas: List<PreguntaPropositoUi> = emptyList(),
) {
    val respondidas: Int get() = preguntas.count { !it.respuesta.isNullOrBlank() }
    val total: Int get() = preguntas.size
}
