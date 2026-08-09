package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.domain.repository.NotaRepository
import javax.inject.Inject

class EliminarNotaUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
) {
    suspend operator fun invoke(notaId: String, usuarioId: String) {
        notaRepository.eliminar(notaId, usuarioId)
    }
}
