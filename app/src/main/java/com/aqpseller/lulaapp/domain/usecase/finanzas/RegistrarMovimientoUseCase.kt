package com.aqpseller.lulaapp.domain.usecase.finanzas

import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class RegistrarMovimientoUseCase @Inject constructor(
    private val finanzasRepository: FinanzasRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        usuarioId: String,
        tipo: TipoMovimientoFinanciero,
        monto: Double,
        categoria: String,
        descripcion: String?,
        fecha: Long,
    ) {
        val movimiento = MovimientoFinanciero(
            id = IdGenerator.newId(),
            espacioId = espacioId,
            tipo = tipo,
            monto = monto,
            categoria = categoria,
            descripcion = descripcion,
            fecha = fecha,
            privacidad = Privacidad.SOLO_YO,
        )
        finanzasRepository.registrarMovimiento(movimiento, usuarioId)
        runCatching { personalSyncRepository.subirMovimientoFinanciero(movimiento) }
    }
}
