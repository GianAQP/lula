package com.aqpseller.lulaapp.domain.usecase.proposito

import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import javax.inject.Inject

class GuardarRespuestaPropositoUseCase @Inject constructor(
    private val propositoPersonalRepository: PropositoPersonalRepository,
) {
    suspend operator fun invoke(espacioId: String, propietario: String, preguntaId: String, respuesta: String) =
        propositoPersonalRepository.guardarRespuesta(espacioId, propietario, preguntaId, respuesta)
}
