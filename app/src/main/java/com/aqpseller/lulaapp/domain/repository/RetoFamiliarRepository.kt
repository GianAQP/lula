package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.ProgresoRetoFamiliar
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import kotlinx.coroutines.flow.Flow

interface RetoFamiliarRepository {
    suspend fun crear(reto: RetoFamiliar, creadoPor: String)

    /** Progreso de hoy de cada Reto del espacio ("x de y ya cumplieron hoy", `02-pantallas.md`). */
    fun observarConProgresoDeHoy(espacioId: String, usuarioId: String): Flow<List<ProgresoRetoFamiliar>>

    suspend fun marcarCumplidoHoy(retoId: String, usuarioId: String, cumplido: Boolean)
}
