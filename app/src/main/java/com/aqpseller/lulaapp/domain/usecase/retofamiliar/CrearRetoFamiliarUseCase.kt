package com.aqpseller.lulaapp.domain.usecase.retofamiliar

import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.FrecuenciaRetoFamiliar
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import javax.inject.Inject

class CrearRetoFamiliarUseCase @Inject constructor(
    private val retoFamiliarRepository: RetoFamiliarRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        nombre: String,
        objetivo: String,
        frecuencia: FrecuenciaRetoFamiliar,
        recompensa: String?,
        participantesIds: List<String>,
        creadoPor: String,
    ) {
        val reto = RetoFamiliar(
            id = IdGenerator.newId(),
            espacioId = espacioId,
            nombre = nombre,
            objetivo = objetivo,
            frecuencia = frecuencia,
            participantesIds = participantesIds,
            recompensa = recompensa,
        )
        retoFamiliarRepository.crear(reto, creadoPor = creadoPor)
        // Un Reto familiar solo existe dentro de un Espacio Familia — siempre se sincroniza,
        // sin necesidad de revisar el tipo de espacio (ver `Plan/12-firebase-auth-y-sync.md`).
        runCatching { espacioSyncRepository.subirReto(espacioId, reto) }
    }
}
