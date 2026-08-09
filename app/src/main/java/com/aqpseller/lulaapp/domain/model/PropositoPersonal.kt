package com.aqpseller.lulaapp.domain.model

/** Respuestas guardadas a las preguntas de `PREGUNTAS_PROPOSITO`, siempre del espacio Personal
 * (igual que Notas/Diario) — se arma de a poco, no hace falta responder todo de una vez. */
data class PropositoPersonal(
    val espacioId: String,
    val propietario: String,
    val respuestas: Map<String, String>,
    val fechaEdicion: Long,
)

enum class SeccionProposito { MISION_VISION, PROPOSITO }

data class PreguntaProposito(val id: String, val texto: String, val seccion: SeccionProposito)

/**
 * Preguntas guiadas para armar Misión/Visión/Propósito de a poco — ver
 * `Plan/10-pendientes.md` y `Plan/08-decisiones-tecnicas.md` (corregido 2026-08-01: las 7
 * preguntas "personales" arman Misión y Visión juntas, no separadas; se sumó una pregunta
 * directa de Propósito porque las 7 solas apuntan a autoconocimiento, no necesariamente al
 * "para qué"). Las 6 preguntas de "armar objetivos" que estaban acá antes en realidad son
 * ayuda para definir una Meta, no para este espacio — se movieron a `CrearMetaScreen`.
 */
val PREGUNTAS_PROPOSITO = listOf(
    PreguntaProposito("apasiona", "¿Qué me apasiona?", SeccionProposito.MISION_VISION),
    PreguntaProposito("valores", "¿Qué valores son importantes para mí?", SeccionProposito.MISION_VISION),
    PreguntaProposito("habilidades", "¿Qué habilidades tengo?", SeccionProposito.MISION_VISION),
    PreguntaProposito("lograr", "¿Qué quiero lograr?", SeccionProposito.MISION_VISION),
    PreguntaProposito("tipo_de_vida", "¿Qué tipo de vida quiero tener?", SeccionProposito.MISION_VISION),
    PreguntaProposito("impacto", "¿Qué impacto quiero tener en mi comunidad?", SeccionProposito.MISION_VISION),
    PreguntaProposito("feliz", "¿Qué me hace feliz?", SeccionProposito.MISION_VISION),
    PreguntaProposito("proposito_directo", "¿Cuál siento que es mi propósito de vida?", SeccionProposito.PROPOSITO),
)
