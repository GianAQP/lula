package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.domain.repository.MetaRepository
import javax.inject.Inject

/** Marca un hito (25/50/75/100) como ya mostrado, para que la tarjeta de celebración en Hoy no
 * se repita — ver `MetaConProgreso.hayHitoNuevo`. */
class MarcarHitoMetaCelebradoUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
) {
    suspend operator fun invoke(metaId: String, hito: Int, usuarioId: String) {
        val meta = metaRepository.obtenerConVinculo(metaId) ?: return
        if (hito <= meta.ultimoHitoCelebrado) return
        metaRepository.actualizar(meta.copy(ultimoHitoCelebrado = hito), usuarioId)
    }
}
