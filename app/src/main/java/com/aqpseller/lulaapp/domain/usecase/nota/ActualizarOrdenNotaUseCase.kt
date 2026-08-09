package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.domain.repository.NotaRepository
import javax.inject.Inject

class ActualizarOrdenNotaUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
) {
    suspend operator fun invoke(notaId: String, nuevoOrden: Int, usuarioId: String) =
        notaRepository.actualizarOrden(notaId, nuevoOrden, usuarioId)
}
