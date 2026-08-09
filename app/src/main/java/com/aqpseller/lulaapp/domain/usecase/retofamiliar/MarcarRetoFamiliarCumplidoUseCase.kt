package com.aqpseller.lulaapp.domain.usecase.retofamiliar

import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import javax.inject.Inject

class MarcarRetoFamiliarCumplidoUseCase @Inject constructor(
    private val retoFamiliarRepository: RetoFamiliarRepository,
) {
    suspend operator fun invoke(retoId: String, usuarioId: String, cumplido: Boolean) =
        retoFamiliarRepository.marcarCumplidoHoy(retoId, usuarioId, cumplido)
}
