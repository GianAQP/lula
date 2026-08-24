package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface ResultadoUnionEspacio {
    data class Exito(val nombreEspacio: String) : ResultadoUnionEspacio
    /** Sin distinguir el motivo (no existe / venció / ya lo usó otra persona) — para quien
     * escanea da igual, el mensaje es siempre "pide uno nuevo". */
    data object CodigoInvalido : ResultadoUnionEspacio
}

/**
 * Reclama un código de invitación escaneado y, si sigue vigente, une a la persona al Espacio
 * Familia de inmediato — sin un paso de "aceptar" aparte, a diferencia de `SolicitudCompartir`
 * (que sigue existiendo para invitar por correo). El código dura poco (ver
 * `EspacioSyncRepository.generarCodigoInvitacion`) para que escanear sea seguro: guardar el QR y
 * usarlo después ya no funciona. Ver `Plan/08-decisiones-tecnicas.md`.
 */
class UnirseAEspacioConCodigoUseCase @Inject constructor(
    private val espacioSyncRepository: EspacioSyncRepository,
    private val espacioRepository: EspacioRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(codigoId: String, usuarioId: String): ResultadoUnionEspacio {
        val codigo = espacioSyncRepository.reclamarCodigoInvitacion(codigoId) ?: return ResultadoUnionEspacio.CodigoInvalido

        // El Espacio puede vivir en el dispositivo de quien invitó, no en el mío — sin este
        // mirror mínimo, agregarMiembro fallaría por la FK hacia `espacio`. Mismo patrón que
        // `AceptarSolicitudCompartirUseCase` (rama ESPACIO).
        espacioRepository.asegurarEspacioMinimo(
            espacioId = codigo.espacioId,
            nombre = codigo.nombreEspacio,
            creadoPor = codigo.deFirebaseUid,
            tipo = TipoEspacio.FAMILIA,
        )
        val miNombre = usuarioRepository.observarUsuario().first()?.nombrePreferido
        espacioRepository.agregarMiembro(espacioId = codigo.espacioId, usuarioId = usuarioId, rol = RolEnEspacio.MIEMBRO, nombre = miNombre)
        runCatching {
            espacioSyncRepository.subirMiembro(
                codigo.espacioId,
                EspacioMiembro(espacioId = codigo.espacioId, usuarioId = usuarioId, rol = RolEnEspacio.MIEMBRO, nombre = miNombre),
            )
            espacioSyncRepository.subirPunteroMiEspacio(codigo.espacioId)
        }
        return ResultadoUnionEspacio.Exito(codigo.nombreEspacio)
    }
}
