package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerSolicitudesEnviadasUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
) {
    operator fun invoke(usuarioId: String): Flow<List<SolicitudCompartir>> =
        solicitudCompartirRepository.observarEnviadasPor(usuarioId)
}
