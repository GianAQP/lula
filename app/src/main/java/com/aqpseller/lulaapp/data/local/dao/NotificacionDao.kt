package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.NotificacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {
    @Insert
    suspend fun insertar(notificacion: NotificacionEntity)

    /** Solo para restaurar desde la nube (idempotente) — la creación local normal usa
     * [insertar], que siempre trae un id nuevo y nunca debería chocar. */
    @Upsert
    suspend fun upsert(notificacion: NotificacionEntity)

    @Query("SELECT * FROM notificacion ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<NotificacionEntity>>

    @Query("SELECT COUNT(*) FROM notificacion WHERE leido = 0")
    fun observarNoLeidas(): Flow<Int>

    @Query("UPDATE notificacion SET leido = 1 WHERE id = :id")
    suspend fun marcarLeida(id: String)
}
