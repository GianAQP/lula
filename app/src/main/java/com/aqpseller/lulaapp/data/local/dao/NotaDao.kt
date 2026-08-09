package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.NotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {
    @Upsert
    suspend fun upsert(nota: NotaEntity)

    /** `orden` es manual (flechas ▲▼ en la lista) — ya no se ordena por fecha. */
    @Query("SELECT * FROM nota WHERE espacioId = :espacioId ORDER BY orden ASC")
    fun observarPorEspacio(espacioId: String): Flow<List<NotaEntity>>

    @Query("SELECT * FROM nota WHERE id = :id")
    suspend fun obtenerPorId(id: String): NotaEntity?

    @Query("SELECT MIN(orden) FROM nota WHERE espacioId = :espacioId")
    suspend fun obtenerOrdenMinimo(espacioId: String): Int?

    @Query("UPDATE nota SET orden = :orden WHERE id = :id")
    suspend fun actualizarOrden(id: String, orden: Int)

    @Query("DELETE FROM nota WHERE id = :id")
    suspend fun eliminar(id: String)
}
