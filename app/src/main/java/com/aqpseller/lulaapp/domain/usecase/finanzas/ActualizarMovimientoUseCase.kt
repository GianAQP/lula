package com.aqpseller.lulaapp.domain.usecase.finanzas

import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import javax.inject.Inject

class ActualizarMovimientoUseCase @Inject constructor(
    private val finanzasRepository: FinanzasRepository,
) {
    suspend operator fun invoke(
        movimientoId: String,
        usuarioId: String,
        tipo: TipoMovimientoFinanciero,
        monto: Double,
        categoria: String,
        descripcion: String?,
        fecha: Long,
    ) {
        finanzasRepository.actualizarMovimiento(movimientoId, tipo, monto, categoria, descripcion, fecha, usuarioId)
    }
}
