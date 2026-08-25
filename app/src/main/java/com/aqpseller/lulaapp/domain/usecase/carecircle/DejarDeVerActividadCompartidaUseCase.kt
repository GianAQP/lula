package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.repository.CareCircleContenidoSyncRepository
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import javax.inject.Inject

/**
 * Del lado de quien acompaña: dejar de ver algo que otra persona le comparte, sin tener que
 * esperar a que ella lo revoque primero. Marca la solicitud como `RECHAZADA` (mismo estado que
 * "rechazar", tiene sentido igual aunque ya estuviera aceptada: "ya no quiero ver esto") y borra
 * de una vez el espejo de contenido — no hace falta esperar a que el celular de quien comparte
 * esté online para que desaparezca de "Lo que me comparten". Ver `Plan/08-decisiones-tecnicas.md`.
 */
class DejarDeVerActividadCompartidaUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
    private val compartirSyncRepository: CompartirSyncRepository,
    private val careCircleContenidoSyncRepository: CareCircleContenidoSyncRepository,
) {
    suspend operator fun invoke(solicitudId: String, miUsuarioId: String) {
        val solicitud = solicitudCompartirRepository.obtenerPorId(solicitudId) ?: return
        solicitudCompartirRepository.responder(solicitudId, EstadoSolicitud.RECHAZADA, miUsuarioId)
        runCatching {
            compartirSyncRepository.subirSolicitud(
                solicitud.copy(estado = EstadoSolicitud.RECHAZADA, fechaRespuesta = DateTimeUtils.ahoraEpochMillis()),
            )
        }
        runCatching { careCircleContenidoSyncRepository.eliminarActividadCompartida(solicitudId) }
    }
}
