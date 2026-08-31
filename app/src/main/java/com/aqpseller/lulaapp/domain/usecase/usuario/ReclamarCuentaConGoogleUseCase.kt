package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.repository.AuthRepository
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.espacio.RestaurarEspaciosFamiliaUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * "Reclamar cuenta": la persona ya usa Lula con el usuario semilla local, y ahora la vincula
 * con Google. Inicia sesión real en Firebase con el ID token, y actualiza esa misma fila local
 * (mismo `id`, ningún FK se toca) — nunca crea un usuario nuevo. Ver
 * `Plan/12-firebase-auth-y-sync.md`, sección 5.
 *
 * También restaura el respaldo personal (Hábitos/Tareas), los Espacios Familia en los que ya
 * era miembro, el perfil (nombre real, horarios), y los Ajustes de pantalla (barra inferior,
 * duración de alarma) si esta cuenta ya tenía algo guardado en la nube desde otro dispositivo —
 * idempotente, no duplica nada si no había nada que traer.
 */
class ReclamarCuentaConGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val compartirSyncRepository: CompartirSyncRepository,
    private val restaurarDatosPersonalesUseCase: RestaurarDatosPersonalesUseCase,
    private val restaurarEspaciosFamiliaUseCase: RestaurarEspaciosFamiliaUseCase,
    private val restaurarAjustesUseCase: RestaurarAjustesUseCase,
) {
    /** true si esta cuenta ya había completado el registro en otro dispositivo — quien llama
     * (el onboarding) lo usa para saltarse el resto del wizard en vez de volver a preguntarlo. */
    suspend operator fun invoke(idToken: String): Boolean {
        val sesion = authRepository.iniciarSesionConGoogle(idToken)
        val usuarioId = checkNotNull(authRepository.usuarioActualId()) { "Usuario no inicializado" }
        usuarioRepository.vincularConGoogle(
            usuarioId = usuarioId,
            correo = checkNotNull(sesion.correo) { "La cuenta de Google no tiene correo" },
            firebaseUid = sesion.firebaseUid,
        )
        // Primero trae lo que ya hubiera en la nube (nombre real, si ya se había registrado
        // antes) — antes de volver a subir, para no pisar un nombre real con el placeholder
        // semilla ("Tú") de este dispositivo si es la primera vez que se abre acá.
        val perfilRemoto = runCatching { compartirSyncRepository.restaurarPerfil(sesion.firebaseUid) }.getOrNull()
        if (perfilRemoto != null) {
            usuarioRepository.aplicarPerfilRemoto(
                usuarioId = usuarioId,
                nombreCompleto = perfilRemoto.nombreCompleto,
                nombrePreferido = perfilRemoto.nombrePreferido,
                horaDesayuno = perfilRemoto.horaDesayuno,
                horaAlmuerzo = perfilRemoto.horaAlmuerzo,
                horaCena = perfilRemoto.horaCena,
                onboardingCompletadoEn = perfilRemoto.onboardingCompletadoEn,
            )
        }
        usuarioRepository.observarUsuario().first()?.let { usuario ->
            runCatching { compartirSyncRepository.subirPerfil(usuario) }
        }
        runCatching { restaurarDatosPersonalesUseCase(usuarioId) }
        runCatching { restaurarEspaciosFamiliaUseCase(usuarioId) }
        runCatching { restaurarAjustesUseCase(sesion.firebaseUid) }
        val yaTeniaRegistroCompleto = perfilRemoto?.onboardingCompletadoEn != null
        return yaTeniaRegistroCompleto
    }
}
