package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.TipoSolicitud
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface ResultadoReclamoCompartir {
    data class Exito(val nombreActividad: String, val deNombre: String) : ResultadoReclamoCompartir
    /** Sin distinguir el motivo (no existe / venció / ya lo usó otra persona) ni si falta cuenta
     * vinculada — para quien escanea da igual, el mensaje es siempre "pide uno nuevo". */
    data object CodigoInvalido : ResultadoReclamoCompartir
}

/**
 * Reclama un código de "Compartir seguimiento" escaneado y, si sigue vigente, crea la solicitud
 * ya `ACEPTADA` directo en Firestore — sin un paso de "aceptar" aparte, a diferencia de compartir
 * por correo/teléfono. Solo escribe en Firestore (no en Room local): el contenido real de la
 * actividad vive en el dispositivo de quien comparte, así que ese lado la va a ver reflejada
 * cuando su propio "Mi círculo de cuidado" vuelva a sincronizar — mismo patrón best-effort de
 * siempre (ver `Plan/08-decisiones-tecnicas.md`).
 */
class ReclamarCodigoCompartirActividadUseCase @Inject constructor(
    private val compartirSyncRepository: CompartirSyncRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(codigoId: String): ResultadoReclamoCompartir {
        val miUsuario = usuarioRepository.observarUsuario().first() ?: return ResultadoReclamoCompartir.CodigoInvalido
        val miCorreo = miUsuario.correo
        if (miCorreo.isNullOrBlank()) return ResultadoReclamoCompartir.CodigoInvalido
        val miNombre = miUsuario.nombrePreferido ?: "Alguien"
        val codigo = compartirSyncRepository.reclamarCodigoCompartir(codigoId, miNombre, miCorreo)
            ?: return ResultadoReclamoCompartir.CodigoInvalido

        val solicitud = SolicitudCompartir(
            id = codigoId,
            de = codigo.deUsuarioId,
            para = miCorreo,
            tieneCuenta = true,
            elementoId = codigo.actividadId,
            contexto = codigo.nombreActividad,
            deNombre = codigo.deNombre,
            tipo = TipoSolicitud.ACTIVIDAD,
            permisos = codigo.permiso,
            estado = EstadoSolicitud.ACEPTADA,
            canalEnvio = null,
            fechaSolicitud = DateTimeUtils.ahoraEpochMillis(),
            fechaRespuesta = DateTimeUtils.ahoraEpochMillis(),
        )
        runCatching { compartirSyncRepository.subirSolicitud(solicitud) }
        return ResultadoReclamoCompartir.Exito(codigo.nombreActividad, codigo.deNombre)
    }
}
