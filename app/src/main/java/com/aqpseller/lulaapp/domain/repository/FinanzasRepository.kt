package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import kotlinx.coroutines.flow.Flow

interface FinanzasRepository {
    suspend fun registrarMovimiento(movimiento: MovimientoFinanciero, usuarioId: String)
    fun observarMovimientosEntrePeriodo(espacioId: String, desde: Long, hasta: Long): Flow<List<MovimientoFinanciero>>
}
