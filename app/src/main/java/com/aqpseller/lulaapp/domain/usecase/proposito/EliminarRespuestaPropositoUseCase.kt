package com.aqpseller.lulaapp.domain.usecase.proposito

import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import javax.inject.Inject

class EliminarRespuestaPropositoUseCase @Inject constructor(
    private val propositoPersonalRepository: PropositoPersonalRepository,
) {
    suspend operator fun invoke(espacioId: String, propietario: String, preguntaId: String) =
        propositoPersonalRepository.eliminarRespuesta(espacioId, propietario, preguntaId)
}
