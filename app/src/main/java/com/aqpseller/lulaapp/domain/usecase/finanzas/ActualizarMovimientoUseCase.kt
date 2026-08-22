package com.aqpseller.lulaapp.domain.usecase.finanzas

import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class ActualizarMovimientoUseCase @Inject constructor(
    private val finanzasRepository: FinanzasRepository,
    private val personalSyncRepository: PersonalSyncRepository,
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
        finanzasRepository.obtenerPorId(movimientoId)?.let { movimiento ->
            runCatching { personalSyncRepository.subirMovimientoFinanciero(movimiento) }
        }
    }
}
