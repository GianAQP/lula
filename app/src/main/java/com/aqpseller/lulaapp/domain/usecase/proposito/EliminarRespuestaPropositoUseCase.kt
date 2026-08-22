package com.aqpseller.lulaapp.domain.usecase.proposito

import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class EliminarRespuestaPropositoUseCase @Inject constructor(
    private val propositoPersonalRepository: PropositoPersonalRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, propietario: String, preguntaId: String) {
        propositoPersonalRepository.eliminarRespuesta(espacioId, propietario, preguntaId)
        propositoPersonalRepository.observar(espacioId).firstOrNull()?.let { proposito ->
            runCatching { personalSyncRepository.subirProposito(proposito) }
        }
    }
}
