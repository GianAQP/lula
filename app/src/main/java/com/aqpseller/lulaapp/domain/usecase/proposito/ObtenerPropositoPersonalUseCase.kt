package com.aqpseller.lulaapp.domain.usecase.proposito

import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerPropositoPersonalUseCase @Inject constructor(
    private val propositoPersonalRepository: PropositoPersonalRepository,
) {
    operator fun invoke(espacioId: String): Flow<PropositoPersonal?> = propositoPersonalRepository.observar(espacioId)
}
