package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import javax.inject.Inject

class RenombrarEspacioFamiliaUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
) {
    suspend operator fun invoke(espacioId: String, nuevoNombre: String, usuarioId: String) =
        espacioRepository.renombrarEspacio(espacioId, nuevoNombre, usuarioId)
}
