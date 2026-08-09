package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import javax.inject.Inject

/** Cambia el espacio activo — null vuelve a Personal (ver "selector de espacio", `02-pantallas.md`). */
class CambiarEspacioActivoUseCase @Inject constructor(
    private val ajustesRepository: AjustesRepository,
) {
    suspend operator fun invoke(espacioId: String?) = ajustesRepository.setEspacioActivoId(espacioId)
}
