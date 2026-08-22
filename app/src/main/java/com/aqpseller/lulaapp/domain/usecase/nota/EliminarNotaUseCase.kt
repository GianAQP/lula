package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.domain.repository.NotaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class EliminarNotaUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(notaId: String, usuarioId: String) {
        notaRepository.eliminar(notaId, usuarioId)
        runCatching { personalSyncRepository.eliminarNota(notaId) }
    }
}
