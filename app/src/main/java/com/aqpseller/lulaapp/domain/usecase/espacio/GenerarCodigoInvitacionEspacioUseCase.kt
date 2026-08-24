package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.CodigoInvitacionEspacio
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Genera un código de invitación de corta duración para un Espacio Familia — pensado para
 * mostrarse como QR y que la otra persona lo escanee y quede dentro de inmediato (ver
 * `UnirseAEspacioConCodigoUseCase`). Ver `Plan/08-decisiones-tecnicas.md`. */
class GenerarCodigoInvitacionEspacioUseCase @Inject constructor(
    private val espacioSyncRepository: EspacioSyncRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(espacioId: String, nombreEspacio: String): CodigoInvitacionEspacio {
        val miNombre = usuarioRepository.observarUsuario().first()?.nombrePreferido ?: "Alguien"
        return espacioSyncRepository.generarCodigoInvitacion(espacioId, nombreEspacio, miNombre)
    }
}
