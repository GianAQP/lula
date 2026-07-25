package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.ActividadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActividadDao {

    @Upsert
    suspend fun upsert(actividad: ActividadEntity)

    @Query("SELECT * FROM actividad WHERE id = :id")
    suspend fun obtenerPorId(id: String): ActividadEntity?

    /** Consulta central de la pantalla Hoy: usa los índices de espacioId y estado, sin joins. */
    @Query("SELECT * FROM actividad WHERE espacioId = :espacioId AND estado != 'OMITIDO' ORDER BY fechaCreacion ASC")
    fun observarActivasDeEspacio(espacioId: String): Flow<List<ActividadEntity>>

    @Query("SELECT * FROM actividad WHERE espacioId = :espacioId AND tipo = :tipo ORDER BY fechaCreacion DESC")
    fun observarPorTipo(espacioId: String, tipo: String): Flow<List<ActividadEntity>>

    @Query("UPDATE actividad SET estado = :estado WHERE id = :id")
    suspend fun actualizarEstado(id: String, estado: String)
}
