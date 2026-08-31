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

    /** Busca en TODO el historial (no solo el rango visible) — para "¿cuándo gasté eso?". Filtra
     * en la base de datos, no trae todo a memoria. Ver `Plan/08-decisiones-tecnicas.md`. */
    @Query("SELECT * FROM finanzas WHERE espacioId = :espacioId AND (categoria LIKE '%' || :consulta || '%' OR descripcion LIKE '%' || :consulta || '%') ORDER BY fecha DESC")
    fun buscar(espacioId: String, consulta: String): Flow<List<FinanzasEntity>>

    @Query("SELECT * FROM finanzas WHERE id = :id")
    suspend fun obtenerPorId(id: String): FinanzasEntity?

    @Query("DELETE FROM finanzas WHERE id = :id")
    suspend fun eliminar(id: String)

    /** Para eliminar un espacio completo (ver `EspacioRepositoryImpl.eliminarEspacio`). */
    @Query("DELETE FROM finanzas WHERE espacioId = :espacioId")
    suspend fun eliminarPorEspacio(espacioId: String)
}
