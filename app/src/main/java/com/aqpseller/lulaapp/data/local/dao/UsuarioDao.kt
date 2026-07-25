package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Upsert
    suspend fun upsert(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuario LIMIT 1")
    suspend fun obtenerUnico(): UsuarioEntity?

    @Query("SELECT * FROM usuario LIMIT 1")
    fun observarUnico(): Flow<UsuarioEntity?>

    @Query("SELECT COUNT(*) FROM usuario")
    suspend fun contar(): Int
}
