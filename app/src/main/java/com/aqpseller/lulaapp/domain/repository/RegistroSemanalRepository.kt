package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import kotlinx.coroutines.flow.Flow

interface RegistroSemanalRepository {
    suspend fun guardarRevision(registro: RegistroSemanal, usuarioId: String)
    suspend fun obtenerPorSemana(espacioId: String, semana: String): RegistroSemanal?

    /** Semanas guardadas, de la más reciente a la más antigua — nunca se borran solas. */
    fun observarHistorial(espacioId: String): Flow<List<RegistroSemanal>>
}
