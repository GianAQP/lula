package com.aqpseller.lulaapp.domain.usecase.finanzas

import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import javax.inject.Inject

class EliminarMovimientoUseCase @Inject constructor(
    private val finanzasRepository: FinanzasRepository,
) {
    suspend operator fun invoke(movimientoId: String, usuarioId: String) {
        finanzasRepository.eliminarMovimiento(movimientoId, usuarioId)
    }
}
