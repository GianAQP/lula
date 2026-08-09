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

    /**
     * Consulta central de la pantalla Hoy: usa los índices de espacioId y activa, sin joins.
     * Para hábitos, `estado` en esta tabla es solo un valor histórico sin significado diario
     * (el cumplimiento real vive en `registro_actividad`) — por eso no se filtra por estado
     * cuando el tipo es HABITO, solo cuando es TAREA (una tarea omitida no debe verse en Hoy).
     */
    @Query("SELECT * FROM actividad WHERE espacioId = :espacioId AND activa = 1 AND (tipo = 'HABITO' OR estado != 'OMITIDO') ORDER BY fechaCreacion ASC")
    fun observarActivasDeEspacio(espacioId: String): Flow<List<ActividadEntity>>

    @Query("SELECT * FROM actividad WHERE espacioId = :espacioId AND tipo = :tipo AND activa = 1 ORDER BY fechaCreacion DESC")
    fun observarPorTipo(espacioId: String, tipo: String): Flow<List<ActividadEntity>>

    @Query("UPDATE actividad SET estado = :estado WHERE id = :id")
    suspend fun actualizarEstado(id: String, estado: String)

    @Query("UPDATE actividad SET estado = :estado, fechaCompletado = :fechaCompletado WHERE id = :id")
    suspend fun actualizarEstadoYFechaCompletado(id: String, estado: String, fechaCompletado: Long?)

    @Query("UPDATE actividad SET activa = :activa WHERE id = :id")
    suspend fun actualizarActiva(id: String, activa: Boolean)

    @Query("DELETE FROM actividad WHERE id = :id")
    suspend fun eliminar(id: String)

    /** Para eliminar un espacio completo (ver `EspacioRepositoryImpl.eliminarEspacio`) — cascada
     * automática a cada tabla de detalle vía `onDelete = CASCADE`. */
    @Query("DELETE FROM actividad WHERE espacioId = :espacioId")
    suspend fun eliminarPorEspacio(espacioId: String)
}
