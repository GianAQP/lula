package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.RegistroDiarioEntity
import com.aqpseller.lulaapp.data.local.entity.RegistroSemanalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDiarioDao {

    @Upsert
    suspend fun upsert(registro: RegistroDiarioEntity)

    @Query("SELECT * FROM registro_diario WHERE espacioId = :espacioId AND fecha = :fecha LIMIT 1")
    suspend fun obtenerPorFecha(espacioId: String, fecha: Long): RegistroDiarioEntity?

    @Query("SELECT * FROM registro_diario WHERE espacioId = :espacioId ORDER BY fecha DESC")
    fun observarHistorial(espacioId: String): Flow<List<RegistroDiarioEntity>>
}

@Dao
interface RegistroSemanalDao {

    @Upsert
    suspend fun upsert(registro: RegistroSemanalEntity)

    @Query("SELECT * FROM registro_semanal WHERE espacioId = :espacioId AND semana = :semana LIMIT 1")
    suspend fun obtenerPorSemana(espacioId: String, semana: String): RegistroSemanalEntity?

    @Query("SELECT * FROM registro_semanal WHERE espacioId = :espacioId ORDER BY semana DESC")
    fun observarHistorial(espacioId: String): Flow<List<RegistroSemanalEntity>>
}
