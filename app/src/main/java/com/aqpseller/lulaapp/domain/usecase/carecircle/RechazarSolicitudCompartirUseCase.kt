package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RechazarSolicitudCompartirUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
    private val compartirSyncRepository: CompartirSyncRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(solicitud: SolicitudCompartir, miUsuarioId: String) {
        val miNombre = usuarioRepository.observarUsuario().first()?.nombrePreferido
        solicitudCompartirRepository.responder(solicitud.id, EstadoSolicitud.RECHAZADA, miUsuarioId)
        runCatching {
            compartirSyncRepository.subirSolicitud(
                solicitud.copy(
                    estado = EstadoSolicitud.RECHAZADA,
                    fechaRespuesta = DateTimeUtils.ahoraEpochMillis(),
                    nombreQuienResponde = miNombre,
                ),
            )
        }
    }
}
