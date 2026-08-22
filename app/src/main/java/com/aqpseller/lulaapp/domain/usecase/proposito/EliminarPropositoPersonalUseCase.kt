package com.aqpseller.lulaapp.domain.usecase.proposito

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import javax.inject.Inject

class EliminarPropositoPersonalUseCase @Inject constructor(
    private val propositoPersonalRepository: PropositoPersonalRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, propietario: String) {
        propositoPersonalRepository.eliminarTodo(espacioId, propietario)
        runCatching {
            personalSyncRepository.subirProposito(
                PropositoPersonal(
                    espacioId = espacioId,
                    propietario = propietario,
                    respuestas = emptyMap(),
                    fechaEdicion = DateTimeUtils.ahoraEpochMillis(),
                ),
            )
        }
    }
}
