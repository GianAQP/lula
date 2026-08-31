package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Notificacion
import kotlinx.coroutines.flow.Flow

interface NotificacionRepository {
    suspend fun registrar(emoji: String, titulo: String, cuerpo: String, solicitudId: String? = null)

    fun observarTodas(): Flow<List<Notificacion>>

    fun observarNoLeidas(): Flow<Int>

    suspend fun marcarLeida(id: String)

    /** Upsert por id, para restaurar desde la nube sin duplicar (ver `RestaurarDatosPersonalesUseCase`). */
    suspend fun restaurarDesdeRemota(notificacion: Notificacion)
}
