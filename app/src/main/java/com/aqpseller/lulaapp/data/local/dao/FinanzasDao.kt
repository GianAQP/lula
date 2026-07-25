package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.FinanzasEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanzasDao {

    @Upsert
    suspend fun upsert(movimiento: FinanzasEntity)

    @Query("SELECT * FROM finanzas WHERE espacioId = :espacioId AND fecha BETWEEN :desde AND :hasta ORDER BY fecha DESC")
    fun observarEntrePeriodo(espacioId: String, desde: Long, hasta: Long): Flow<List<FinanzasEntity>>
}
