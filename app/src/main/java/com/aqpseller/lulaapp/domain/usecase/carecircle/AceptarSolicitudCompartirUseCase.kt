package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.TipoConexion
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.model.TipoSolicitud
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.ConexionRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Acepta una solicitud que alguien más me envió: marca `ACEPTADA`, crea (o reutiliza) la
 * `Conexion` entre ambos, y según [SolicitudCompartir.tipo]:
 * - ACTIVIDAD: si vive en este dispositivo (soy quien la compartió), le da acceso ahí mismo.
 * - ESPACIO: me agrega como miembro del Espacio Familia al que me invitaron.
 * Nunca automático, siempre requiere este paso explícito (ver `Plan/01-arquitectura.md`).
 *
 * Alcance de esta ronda para ACTIVIDAD: solo sincroniza la solicitud/conexión, no el contenido
 * del hábito o tarea compartido — si me la compartieron a mí, todavía no veo el detalle real en
 * mi dispositivo (queda documentado en `Plan/10-pendientes.md`).
 */
class AceptarSolicitudCompartirUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
    private val actividadRepository: ActividadRepository,
    private val espacioRepository: EspacioRepository,
    private val conexionRepository: ConexionRepository,
    private val compartirSyncRepository: CompartirSyncRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(solicitud: SolicitudCompartir, miUsuarioId: String) {
        solicitudCompartirRepository.responder(solicitud.id, EstadoSolicitud.ACEPTADA, miUsuarioId)
        conexionRepository.crearSiNoExiste(
            usuarioA = solicitud.de,
            usuarioB = miUsuarioId,
            tipo = TipoConexion.CUIDADOR,
            origenSolicitudId = solicitud.id,
        )
        when (solicitud.tipo) {
            TipoSolicitud.ACTIVIDAD -> actividadRepository.agregarPermisoCompartido(
                actividadId = solicitud.elementoId,
                concederA = miUsuarioId,
                permiso = solicitud.permisos,
                usuarioId = miUsuarioId,
            )
            TipoSolicitud.ESPACIO -> {
                // El Espacio puede vivir en el dispositivo de quien invitó, no en el mío —
                // sin este mirror mínimo, agregarMiembro fallaría por la FK hacia `espacio`.
                espacioRepository.asegurarEspacioMinimo(
                    espacioId = solicitud.elementoId,
                    nombre = solicitud.contexto,
                    creadoPor = solicitud.de,
                    tipo = TipoEspacio.FAMILIA,
                )
                val miNombre = usuarioRepository.observarUsuario().first()?.nombrePreferido
                espacioRepository.agregarMiembro(
                    espacioId = solicitud.elementoId,
                    usuarioId = miUsuarioId,
                    rol = RolEnEspacio.MIEMBRO,
                    nombre = miNombre,
                )
                runCatching {
                    espacioSyncRepository.subirMiembro(
                        solicitud.elementoId,
                        EspacioMiembro(espacioId = solicitud.elementoId, usuarioId = miUsuarioId, rol = RolEnEspacio.MIEMBRO, nombre = miNombre),
                    )
                    espacioSyncRepository.subirPunteroMiEspacio(solicitud.elementoId)
                }
            }
        }
        runCatching {
            compartirSyncRepository.subirSolicitud(
                solicitud.copy(estado = EstadoSolicitud.ACEPTADA, fechaRespuesta = DateTimeUtils.ahoraEpochMillis()),
            )
        }
    }
}
