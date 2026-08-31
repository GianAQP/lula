package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Un evento real de invitación, para avisar con una notificación local — no toda actualización
 * del listener de Firestore es un evento (la mayoría son solo el mismo estado ya conocido
 * llegando de nuevo). Ver `Plan/08-decisiones-tecnicas.md`. */
sealed interface EventoSolicitud {
    /** Alguien me invitó (a mí, `para`) y todavía no la había visto — la tengo PENDIENTE por
     * primera vez. */
    data class NuevaRecibida(val solicitud: SolicitudCompartir) : EventoSolicitud

    /** Una solicitud que YO envié (`de` == yo) cambió de PENDIENTE a ACEPTADA/RECHAZADA. */
    data class Respondida(val solicitud: SolicitudCompartir) : EventoSolicitud
}

/**
 * Envoltorio de `escucharSolicitudes` que compara cada solicitud entrante contra lo que ya
 * había en Room (única fuente de verdad, ver `Plan/01-arquitectura.md`) para detectar solo los
 * cambios reales — así una notificación se dispara una única vez, nunca de nuevo al reabrir la
 * app o reconectar el listener, sin necesitar una bandera aparte de "ya se avisó" (una vez
 * aplicado el cambio a Room, la próxima comparación ya no encuentra diferencia). Pensado para
 * correr globalmente mientras la app esté abierta (ver `TopBarStatsViewModel`), igual que
 * "Recordarle". Ver `Plan/08-decisiones-tecnicas.md`.
 */
class SincronizarYDetectarEventosSolicitudesUseCase @Inject constructor(
    private val compartirSyncRepository: CompartirSyncRepository,
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
) {
    operator fun invoke(miUsuarioId: String, miCorreo: String): Flow<EventoSolicitud> = flow {
        compartirSyncRepository.escucharSolicitudes(miUsuarioId, miCorreo).collect { remotas ->
            remotas.forEach { remota ->
                val previa = solicitudCompartirRepository.obtenerPorId(remota.id)
                solicitudCompartirRepository.crear(remota, miUsuarioId)
                when {
                    previa == null && remota.para == miCorreo && remota.estado == EstadoSolicitud.PENDIENTE ->
                        emit(EventoSolicitud.NuevaRecibida(remota))
                    previa != null && previa.estado == EstadoSolicitud.PENDIENTE &&
                        remota.de == miUsuarioId && remota.estado != EstadoSolicitud.PENDIENTE ->
                        emit(EventoSolicitud.Respondida(remota))
                }
            }
        }
    }
}
