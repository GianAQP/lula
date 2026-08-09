package com.aqpseller.lulaapp.domain.usecase.retofamiliar

import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.FrecuenciaRetoFamiliar
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import javax.inject.Inject

class CrearRetoFamiliarUseCase @Inject constructor(
    private val retoFamiliarRepository: RetoFamiliarRepository,
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
        retoFamiliarRepository.crear(
            RetoFamiliar(
                id = IdGenerator.newId(),
                espacioId = espacioId,
                nombre = nombre,
                objetivo = objetivo,
                frecuencia = frecuencia,
                participantesIds = participantesIds,
                recompensa = recompensa,
            ),
            creadoPor = creadoPor,
        )
    }
}
