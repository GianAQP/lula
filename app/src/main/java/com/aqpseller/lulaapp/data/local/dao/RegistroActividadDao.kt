package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.RegistroActividadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroActividadDao {

    @Upsert
    suspend fun upsert(registro: RegistroActividadEntity)

    @Query("SELECT * FROM registro_actividad WHERE actividadId = :actividadId AND fecha = :fecha LIMIT 1")
    suspend fun obtenerPorActividadIdYFecha(actividadId: String, fecha: Long): RegistroActividadEntity?

    /**
     * Flow atado a la tabla `registro_actividad` — se usa como "señal" para que
     * `observarActividadesDeEspacio` se vuelva a emitir cuando se marca/desmarca un hábito.
     * Sin esto, Room solo invalida el Flow de `actividad` cuando cambia `actividad`, y
     * marcar un hábito escribe en esta tabla aparte — el checkbox no se actualizaba en vivo.
     */
    @Query("SELECT * FROM registro_actividad WHERE fecha = :fecha")
    fun observarPorFecha(fecha: Long): Flow<List<RegistroActividadEntity>>

    @Query("SELECT * FROM registro_actividad WHERE actividadId IN (:actividadIds) AND fecha = :fecha")
    suspend fun obtenerPorActividadIdsYFecha(actividadIds: List<String>, fecha: Long): List<RegistroActividadEntity>

    @Query("SELECT * FROM registro_actividad WHERE actividadId = :actividadId AND fecha BETWEEN :desde AND :hasta ORDER BY fecha ASC")
    suspend fun obtenerPorActividadIdYRango(actividadId: String, desde: Long, hasta: Long): List<RegistroActividadEntity>

    @Query("SELECT * FROM registro_actividad WHERE actividadId IN (:actividadIds) AND fecha BETWEEN :desde AND :hasta ORDER BY fecha ASC")
    suspend fun obtenerPorActividadIdsYRango(actividadIds: List<String>, desde: Long, hasta: Long): List<RegistroActividadEntity>
}
