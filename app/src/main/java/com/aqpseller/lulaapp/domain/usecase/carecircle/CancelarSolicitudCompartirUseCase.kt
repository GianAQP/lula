package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import javax.inject.Inject

class CancelarSolicitudCompartirUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
) {
    suspend operator fun invoke(solicitudId: String, usuarioId: String) {
        solicitudCompartirRepository.cancelar(solicitudId, usuarioId)
    }
}
