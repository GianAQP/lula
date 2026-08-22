package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.repository.AuthRepository
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * "Reclamar cuenta": la persona ya usa Lula con el usuario semilla local, y ahora la vincula
 * con Google. Inicia sesión real en Firebase con el ID token, y actualiza esa misma fila local
 * (mismo `id`, ningún FK se toca) — nunca crea un usuario nuevo. Ver
 * `Plan/12-firebase-auth-y-sync.md`, sección 5.
 *
 * También restaura el respaldo personal (Hábitos/Tareas) si esta cuenta ya tenía uno guardado
 * en la nube desde otro dispositivo — idempotente, no duplica nada si no había nada que traer.
 */
class ReclamarCuentaConGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val compartirSyncRepository: CompartirSyncRepository,
    private val restaurarDatosPersonalesUseCase: RestaurarDatosPersonalesUseCase,
) {
    suspend operator fun invoke(idToken: String) {
        val sesion = authRepository.iniciarSesionConGoogle(idToken)
        val usuarioId = checkNotNull(authRepository.usuarioActualId()) { "Usuario no inicializado" }
        usuarioRepository.vincularConGoogle(
            usuarioId = usuarioId,
            correo = checkNotNull(sesion.correo) { "La cuenta de Google no tiene correo" },
            firebaseUid = sesion.firebaseUid,
        )
        usuarioRepository.observarUsuario().first()?.let { usuario ->
            runCatching { compartirSyncRepository.subirPerfil(usuario) }
        }
        runCatching { restaurarDatosPersonalesUseCase(usuarioId) }
    }
}
