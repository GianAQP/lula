package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.AreaDeVidaEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioMiembroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EspacioDao {

    @Upsert
    suspend fun upsert(espacio: EspacioEntity)

    @Query("SELECT * FROM espacio WHERE creadoPor = :usuarioId AND tipo = 'PERSONAL' LIMIT 1")
    suspend fun obtenerEspacioPersonal(usuarioId: String): EspacioEntity?

    @Query("SELECT * FROM espacio WHERE id = :id")
    fun observarPorId(id: String): Flow<EspacioEntity?>

    @Query("SELECT COUNT(*) FROM espacio")
    suspend fun contar(): Int
}

@Dao
interface EspacioMiembroDao {

    @Upsert
    suspend fun upsert(miembro: EspacioMiembroEntity)

    @Query("SELECT * FROM espacio_miembro WHERE espacioId = :espacioId")
    fun observarMiembros(espacioId: String): Flow<List<EspacioMiembroEntity>>
}

@Dao
interface AreaDeVidaDao {

    @Upsert
    suspend fun upsertTodas(areas: List<AreaDeVidaEntity>)

    @Query("SELECT * FROM area_de_vida WHERE activa = 1")
    fun observarActivas(): Flow<List<AreaDeVidaEntity>>

    @Query("SELECT COUNT(*) FROM area_de_vida")
    suspend fun contar(): Int
}
