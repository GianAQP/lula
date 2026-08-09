package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.AuthRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import javax.inject.Inject

/**
 * Punto único de resolución de "quién soy y en qué espacio estoy" — cada ViewModel de
 * feature lo llama en vez de resolver usuarioId/espacioId por su cuenta (una sola fuente de
 * verdad, ver `Plan/08-decisiones-tecnicas.md`). Requiere que
 * `AsegurarDatosSemillaUseCase` ya se haya ejecutado.
 */
class ObtenerSesionActualUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val espacioRepository: EspacioRepository,
    private val ajustesRepository: AjustesRepository,
) {
    /**
     * El espacio activo es Personal por defecto, salvo que haya uno elegido en Ajustes (ver
     * "selector de espacio", `Plan/02-pantallas.md`) — siempre se valida que el usuario siga
     * siendo miembro de ese espacio antes de usarlo, nunca se confía a ciegas en el id guardado
     * (si el espacio se borró o el usuario ya no pertenece, cae de nuevo a Personal).
     */
    suspend operator fun invoke(): SesionActual {
        val usuarioId = checkNotNull(authRepository.usuarioActualId()) { "Usuario no inicializado" }
        val personal = checkNotNull(espacioRepository.obtenerEspacioPersonal(usuarioId)) { "Espacio no inicializado" }
        val activoId = ajustesRepository.obtenerEspacioActivoId()
        val espacio = activoId?.let { espacioRepository.obtenerEspacioSiEsMiembro(it, usuarioId) } ?: personal
        return SesionActual(usuarioId = usuarioId, espacioId = espacio.id, espacioPersonalId = personal.id)
    }
}
