package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import kotlinx.coroutines.flow.Flow

interface FinanzasRepository {
    suspend fun registrarMovimiento(movimiento: MovimientoFinanciero, usuarioId: String)
    suspend fun actualizarMovimiento(
        movimientoId: String,
        tipo: TipoMovimientoFinanciero,
        monto: Double,
        categoria: String,
        descripcion: String?,
        fecha: Long,
        usuarioId: String,
    )
    suspend fun eliminarMovimiento(movimientoId: String, usuarioId: String)
    suspend fun obtenerPorId(movimientoId: String): MovimientoFinanciero?
    fun observarMovimientosEntrePeriodo(espacioId: String, desde: Long, hasta: Long): Flow<List<MovimientoFinanciero>>

    /** Busca en TODO el historial, no solo un período — para encontrar cuándo se gastó/recibió
     * algo. Vacío (no todos) si `consulta` está en blanco. */
    fun buscarMovimientos(espacioId: String, consulta: String): Flow<List<MovimientoFinanciero>>
}
