package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ProgresoRetoFamiliar
import com.aqpseller.lulaapp.domain.model.RetoFamiliar
import kotlinx.coroutines.flow.Flow

interface RetoFamiliarRepository {
    suspend fun crear(reto: RetoFamiliar, creadoPor: String)

    /** Progreso de hoy de cada Reto del espacio ("x de y ya cumplieron hoy", `02-pantallas.md`). */
    fun observarConProgresoDeHoy(espacioId: String, usuarioId: String): Flow<List<ProgresoRetoFamiliar>>

    suspend fun marcarCumplidoHoy(retoId: String, usuarioId: String, cumplido: Boolean)

    /** Aplica el estado remoto de un Reto familiar compartido — upsert puro, nunca vuelve a
     * subir a Firestore. Ver `Plan/12-firebase-auth-y-sync.md`. */
    suspend fun mergeRemoto(reto: RetoFamiliar)

    /** Igual que [mergeRemoto] pero para un registro de "cumplido hoy" de otro participante. */
    suspend fun mergeRegistroRemoto(retoId: String, usuarioId: String, fecha: Long, estado: EstadoActividad)
}
