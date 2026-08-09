package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import javax.inject.Inject

class ObtenerNotaUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
) {
    suspend operator fun invoke(notaId: String): Nota? = notaRepository.obtenerPorId(notaId)
}
