package com.aqpseller.lulaapp.core.utils

/**
 * Estado inicial de progresión de un Hábito — solo aplica si el usuario configuró los 4
 * campos (duración inicial/objetivo, incremento, cada cuántos días revisar). Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
fun calcularProgresionInicial(
    duracionInicialMin: Int?,
    duracionObjetivoMin: Int?,
    incrementoMin: Int?,
    frecuenciaRevisionDias: Int?,
): Pair<Int?, Long?> {
    if (duracionInicialMin == null || duracionObjetivoMin == null || incrementoMin == null || frecuenciaRevisionDias == null) {
        return null to null
    }
    val proximaRevision = DateTimeUtils.hoy().toEpochDays().toLong() + frecuenciaRevisionDias
    return duracionInicialMin to proximaRevision
}
