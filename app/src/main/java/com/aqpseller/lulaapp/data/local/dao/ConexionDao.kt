package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.ConexionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConexionDao {
    @Upsert
    suspend fun upsert(conexion: ConexionEntity)

    @Query("SELECT * FROM conexion WHERE usuarioA = :usuarioId OR usuarioB = :usuarioId")
    fun observarConexionesDe(usuarioId: String): Flow<List<ConexionEntity>>

    @Query(
        "SELECT * FROM conexion WHERE (usuarioA = :usuarioA AND usuarioB = :usuarioB) " +
            "OR (usuarioA = :usuarioB AND usuarioB = :usuarioA) LIMIT 1",
    )
    suspend fun obtenerEntre(usuarioA: String, usuarioB: String): ConexionEntity?
}
