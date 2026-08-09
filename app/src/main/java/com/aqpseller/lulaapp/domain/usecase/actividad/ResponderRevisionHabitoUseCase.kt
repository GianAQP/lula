package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

/**
 * Responde la tarjeta de progresión ("¿Aumentamos?") — Lula nunca decide sola, el usuario
 * elige una de las 3 opciones de `02-pantallas.md`.
 */
class ResponderRevisionHabitoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend fun subir(actividadId: String, duracionActualMin: Int, incrementoMin: Int, duracionObjetivoMin: Int, frecuenciaRevisionDias: Int, usuarioId: String) {
        val nuevaDuracion = (duracionActualMin + incrementoMin).coerceAtMost(duracionObjetivoMin)
        val proximaRevision = DateTimeUtils.hoy().toEpochDays().toLong() + frecuenciaRevisionDias
        actividadRepository.actualizarProgresionHabito(actividadId, nuevaDuracion, proximaRevision, usuarioId)
    }

    suspend fun mantener(actividadId: String, duracionActualMin: Int, frecuenciaRevisionDias: Int, usuarioId: String) {
        val proximaRevision = DateTimeUtils.hoy().toEpochDays().toLong() + frecuenciaRevisionDias
        actividadRepository.actualizarProgresionHabito(actividadId, duracionActualMin, proximaRevision, usuarioId)
    }

    suspend fun recordarDespues(actividadId: String, duracionActualMin: Int, usuarioId: String) {
        val manana = DateTimeUtils.hoy().toEpochDays().toLong() + 1
        actividadRepository.actualizarProgresionHabito(actividadId, duracionActualMin, manana, usuarioId)
    }
}
