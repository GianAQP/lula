package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import javax.inject.Inject

/** Borra el espacio Familia y todo lo que tenía dentro — para siempre. Si era el espacio
 * activo, vuelve a Personal de una (nunca se queda apuntando a un espacio que ya no existe). */
class EliminarEspacioFamiliaUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
    private val ajustesRepository: AjustesRepository,
) {
    suspend operator fun invoke(espacioId: String, usuarioId: String) {
        espacioRepository.eliminarEspacio(espacioId, usuarioId)
        if (ajustesRepository.obtenerEspacioActivoId() == espacioId) {
            ajustesRepository.setEspacioActivoId(null)
        }
    }
}
