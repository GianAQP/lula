package com.aqpseller.lulaapp.domain.model

/**
 * Usuario + espacio resueltos, usados por todos los ViewModels de features.
 * `espacioId` es el espacio ACTIVO (Personal por defecto, o el que el usuario haya elegido con
 * el selector de espacio — ver `Plan/08-decisiones-tecnicas.md`). `espacioPersonalId` es
 * siempre el Personal, sin importar cuál esté activo — Notas y Diario lo usan a propósito en
 * vez de `espacioId`, porque son privados por naturaleza (viven detrás de Zona Privada) y no
 * tiene sentido que "sigan" al espacio Familia.
 */
data class SesionActual(
    val usuarioId: String,
    val espacioId: String,
    val espacioPersonalId: String,
)
