package com.aqpseller.lulaapp.domain.usecase.finanzas

import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import javax.inject.Inject

class ObtenerMovimientoUseCase @Inject constructor(
    private val finanzasRepository: FinanzasRepository,
) {
    suspend operator fun invoke(movimientoId: String): MovimientoFinanciero? = finanzasRepository.obtenerPorId(movimientoId)
}
