package com.aqpseller.lulaapp.domain.usecase.diario

import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class EliminarEntradaDiarioUseCase @Inject constructor(
    private val entradaDiarioRepository: EntradaDiarioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(entradaId: String, usuarioId: String) {
        entradaDiarioRepository.eliminar(entradaId, usuarioId)
        runCatching { personalSyncRepository.eliminarEntradaDiario(entradaId) }
    }
}
