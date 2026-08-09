package com.aqpseller.lulaapp.domain.usecase.proposito

import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import javax.inject.Inject

class EliminarPropositoPersonalUseCase @Inject constructor(
    private val propositoPersonalRepository: PropositoPersonalRepository,
) {
    suspend operator fun invoke(espacioId: String, propietario: String) =
        propositoPersonalRepository.eliminarTodo(espacioId, propietario)
}
