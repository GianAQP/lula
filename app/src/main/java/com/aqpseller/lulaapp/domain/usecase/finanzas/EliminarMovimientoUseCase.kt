package com.aqpseller.lulaapp.domain.usecase.finanzas

import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class EliminarMovimientoUseCase @Inject constructor(
    private val finanzasRepository: FinanzasRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(movimientoId: String, usuarioId: String) {
        finanzasRepository.eliminarMovimiento(movimientoId, usuarioId)
        runCatching { personalSyncRepository.eliminarMovimientoFinanciero(movimientoId) }
    }
}
