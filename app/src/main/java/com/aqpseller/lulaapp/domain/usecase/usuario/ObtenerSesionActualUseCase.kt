package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.model.SesionActual
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
) {
    suspend operator fun invoke(): SesionActual {
        val usuarioId = checkNotNull(authRepository.usuarioActualId()) { "Usuario no inicializado" }
        val espacio = checkNotNull(espacioRepository.obtenerEspacioPersonal(usuarioId)) { "Espacio no inicializado" }
        return SesionActual(usuarioId = usuarioId, espacioId = espacio.id)
    }
}
