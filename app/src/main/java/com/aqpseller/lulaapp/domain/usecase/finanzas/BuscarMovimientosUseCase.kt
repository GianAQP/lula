package com.aqpseller.lulaapp.domain.usecase.finanzas

import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BuscarMovimientosUseCase @Inject constructor(
    private val finanzasRepository: FinanzasRepository,
) {
    operator fun invoke(espacioId: String, consulta: String): Flow<List<MovimientoFinanciero>> =
        finanzasRepository.buscarMovimientos(espacioId, consulta)
}
