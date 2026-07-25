package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.RegistroDiario
import kotlinx.coroutines.flow.Flow

interface RegistroDiarioRepository {
    suspend fun cerrarDia(registro: RegistroDiario, usuarioId: String)
    suspend fun obtenerPorFecha(espacioId: String, fecha: Long): RegistroDiario?
    fun observarHistorial(espacioId: String): Flow<List<RegistroDiario>>
}
