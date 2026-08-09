package com.aqpseller.lulaapp.domain.usecase.diario

import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import javax.inject.Inject

class EliminarEntradaDiarioUseCase @Inject constructor(
    private val entradaDiarioRepository: EntradaDiarioRepository,
) {
    suspend operator fun invoke(entradaId: String, usuarioId: String) {
        entradaDiarioRepository.eliminar(entradaId, usuarioId)
    }
}
