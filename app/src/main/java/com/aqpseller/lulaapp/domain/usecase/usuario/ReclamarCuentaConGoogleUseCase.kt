package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.repository.AuthRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import javax.inject.Inject

/**
 * "Reclamar cuenta": la persona ya usa Lula con el usuario semilla local, y ahora la vincula
 * con Google. Inicia sesión real en Firebase con el ID token, y actualiza esa misma fila local
 * (mismo `id`, ningún FK se toca) — nunca crea un usuario nuevo. Ver
 * `Plan/12-firebase-auth-y-sync.md`, sección 5.
 */
class ReclamarCuentaConGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(idToken: String) {
        val sesion = authRepository.iniciarSesionConGoogle(idToken)
        val usuarioId = checkNotNull(authRepository.usuarioActualId()) { "Usuario no inicializado" }
        usuarioRepository.vincularConGoogle(
            usuarioId = usuarioId,
            correo = checkNotNull(sesion.correo) { "La cuenta de Google no tiene correo" },
            firebaseUid = sesion.firebaseUid,
        )
    }
}
