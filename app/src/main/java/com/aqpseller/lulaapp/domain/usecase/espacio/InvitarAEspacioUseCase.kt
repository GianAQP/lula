package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.TipoSolicitud
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Invita a una persona real a un Espacio Familia — reutiliza exactamente la misma
 * infraestructura de solicitud + aceptación + sync de Círculo de cuidado
 * (`SolicitudCompartir` con `tipo = ESPACIO`), en vez de duplicarla. Ver
 * `Plan/12-firebase-auth-y-sync.md`.
 */
class InvitarAEspacioUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
    private val usuarioRepository: UsuarioRepository,
    private val compartirSyncRepository: CompartirSyncRepository,
) {
    suspend operator fun invoke(deUsuarioId: String, espacioId: String, nombreEspacio: String, contactoDestinatario: String) {
        val miNombre = usuarioRepository.observarUsuario().first()?.nombrePreferido ?: "Alguien"
        val solicitud = SolicitudCompartir(
            id = IdGenerator.newId(),
            de = deUsuarioId,
            para = contactoDestinatario,
            tieneCuenta = false,
            elementoId = espacioId,
            contexto = nombreEspacio,
            deNombre = miNombre,
            tipo = TipoSolicitud.ESPACIO,
            permisos = PermisoCompartir.PUEDE_VER_Y_RECORDAR, // sin efecto para ESPACIO, ver SolicitudCompartir
            estado = EstadoSolicitud.PENDIENTE,
            canalEnvio = null,
            fechaSolicitud = DateTimeUtils.ahoraEpochMillis(),
        )
        solicitudCompartirRepository.crear(solicitud, deUsuarioId)
        runCatching { compartirSyncRepository.subirSolicitud(solicitud) }
    }
}
