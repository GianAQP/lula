package com.aqpseller.lulaapp.domain.usecase.retofamiliar

import com.aqpseller.lulaapp.domain.model.ProgresoRetoFamiliar
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerRetosFamiliaresUseCase @Inject constructor(
    private val retoFamiliarRepository: RetoFamiliarRepository,
) {
    operator fun invoke(espacioId: String, usuarioId: String): Flow<List<ProgresoRetoFamiliar>> =
        retoFamiliarRepository.observarConProgresoDeHoy(espacioId, usuarioId)
}
