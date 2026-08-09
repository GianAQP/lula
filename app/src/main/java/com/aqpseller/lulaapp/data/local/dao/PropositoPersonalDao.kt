package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.PropositoPersonalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropositoPersonalDao {
    @Upsert
    suspend fun upsert(proposito: PropositoPersonalEntity)

    @Query("SELECT * FROM proposito_personal WHERE espacioId = :espacioId")
    fun observarPorEspacio(espacioId: String): Flow<PropositoPersonalEntity?>

    @Query("SELECT * FROM proposito_personal WHERE espacioId = :espacioId")
    suspend fun obtenerPorEspacio(espacioId: String): PropositoPersonalEntity?

    @Query("DELETE FROM proposito_personal WHERE espacioId = :espacioId")
    suspend fun eliminar(espacioId: String)
}
