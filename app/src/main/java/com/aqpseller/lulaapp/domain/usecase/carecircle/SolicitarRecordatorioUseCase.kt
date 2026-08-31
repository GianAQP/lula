package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** "Recordarle" desde "Lo que me comparten" — solo tiene sentido si el permiso es
 * `PUEDE_VER_Y_RECORDAR` (la pantalla decide eso, este caso de uso no lo vuelve a validar).
 * Best-effort: si la cuenta no está vinculada, no hace nada (ver
 * `CompartirSyncRepository.solicitarRecordatorio`). Ver `Plan/08-decisiones-tecnicas.md`. */
class SolicitarRecordatorioUseCase @Inject constructor(
    private val compartirSyncRepository: CompartirSyncRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(actividadId: String, nombreActividad: String, paraFirebaseUid: String) {
        val miNombre = usuarioRepository.observarUsuario().first()?.nombrePreferido ?: "Alguien"
        runCatching { compartirSyncRepository.solicitarRecordatorio(actividadId, nombreActividad, miNombre, paraFirebaseUid) }
    }
}
