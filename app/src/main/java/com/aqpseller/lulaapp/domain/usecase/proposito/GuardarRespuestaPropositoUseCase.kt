package com.aqpseller.lulaapp.domain.usecase.proposito

import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GuardarRespuestaPropositoUseCase @Inject constructor(
    private val propositoPersonalRepository: PropositoPersonalRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, propietario: String, preguntaId: String, respuesta: String) {
        propositoPersonalRepository.guardarRespuesta(espacioId, propietario, preguntaId, respuesta)
        propositoPersonalRepository.observar(espacioId).first()?.let { proposito ->
            runCatching { personalSyncRepository.subirProposito(proposito) }
        }
    }
}
