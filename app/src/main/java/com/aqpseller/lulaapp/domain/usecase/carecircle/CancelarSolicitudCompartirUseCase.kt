package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.repository.CareCircleContenidoSyncRepository
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import javax.inject.Inject

/** También sirve para "revocar acceso" a algo ya `ACEPTADA` (mismo botón en la UI, ver
 * `CareCircleScreen`) — por eso además borra el espejo de contenido si lo había, no solo la
 * solicitud en sí; si no, quien acompañaba seguiría viendo la actividad en "Lo que me
 * comparten" aunque el acceso ya se haya cortado. */
class CancelarSolicitudCompartirUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
    private val compartirSyncRepository: CompartirSyncRepository,
    private val careCircleContenidoSyncRepository: CareCircleContenidoSyncRepository,
) {
    suspend operator fun invoke(solicitudId: String, usuarioId: String) {
        solicitudCompartirRepository.cancelar(solicitudId, usuarioId)
        runCatching { compartirSyncRepository.eliminarSolicitud(solicitudId) }
        runCatching { careCircleContenidoSyncRepository.eliminarActividadCompartida(solicitudId) }
    }
}
