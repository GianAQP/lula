package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Salir de un Espacio Familia del que ya soy miembro — el contenido que ya existía (tareas,
 * retos) se queda tal cual, solo se corta mi propia membresía. Si era el espacio activo, vuelve
 * a Personal solo. Ver `Plan/08-decisiones-tecnicas.md`. */
class SalirDeEspacioFamiliaUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
    private val ajustesRepository: AjustesRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(espacioId: String, usuarioId: String) {
        espacioRepository.eliminarMiembro(espacioId, usuarioId, usuarioId)
        if (ajustesRepository.obtenerEspacioActivoId() == espacioId) {
            ajustesRepository.setEspacioActivoId(null)
        }
        val miFirebaseUid = usuarioRepository.observarUsuario().first()?.firebaseUid
        if (miFirebaseUid != null) {
            runCatching { espacioSyncRepository.eliminarMiembro(espacioId, miFirebaseUid) }
        }
    }
}
