package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.ListaEjecucionEntity
import com.aqpseller.lulaapp.data.local.entity.ListaEntity
import com.aqpseller.lulaapp.data.local.entity.ListaItemEntity
import kotlinx.coroutines.flow.Flow

/** Fila agregada para la pantalla de lista de Listas — evita N+1 consultando conteos con un solo `JOIN`. */
data class ListaConConteoRow(
    val id: String,
    val nombre: String,
    val total: Int,
    val marcados: Int,
)

@Dao
interface ListaDao {
    @Upsert
    suspend fun upsert(lista: ListaEntity)

    @Query("SELECT * FROM lista WHERE id = :id")
    suspend fun obtenerPorId(id: String): ListaEntity?

    @Query("SELECT * FROM lista WHERE id = :id")
    fun observarPorId(id: String): Flow<ListaEntity?>

    @Query(
        """
        SELECT lista.id as id, lista.nombre as nombre,
               COUNT(lista_item.id) as total,
               COALESCE(SUM(lista_item.marcado), 0) as marcados
        FROM lista
        LEFT JOIN lista_item ON lista_item.listaId = lista.id
        WHERE lista.espacioId = :espacioId
        GROUP BY lista.id
        ORDER BY lista.fechaCreacion DESC
        """,
    )
    fun observarConConteo(espacioId: String): Flow<List<ListaConConteoRow>>

    @Query("DELETE FROM lista WHERE id = :id")
    suspend fun eliminar(id: String)

    /** Para eliminar un espacio completo (ver `EspacioRepositoryImpl.eliminarEspacio`). */
    @Query("DELETE FROM lista WHERE espacioId = :espacioId")
    suspend fun eliminarPorEspacio(espacioId: String)
}

@Dao
interface ListaItemDao {
    @Upsert
    suspend fun upsert(item: ListaItemEntity)

    @Query("SELECT * FROM lista_item WHERE id = :id")
    suspend fun obtenerPorId(id: String): ListaItemEntity?

    @Query("SELECT * FROM lista_item WHERE listaId = :listaId ORDER BY orden ASC")
    suspend fun obtenerPorLista(listaId: String): List<ListaItemEntity>

    @Query("SELECT * FROM lista_item WHERE listaId = :listaId ORDER BY orden ASC")
    fun observarPorLista(listaId: String): Flow<List<ListaItemEntity>>

    @Query("UPDATE lista_item SET marcado = 0 WHERE listaId = :listaId")
    suspend fun desmarcarTodos(listaId: String)

    @Query("DELETE FROM lista_item WHERE id = :id")
    suspend fun eliminar(id: String)
}

@Dao
interface ListaEjecucionDao {
    @Upsert
    suspend fun upsert(ejecucion: ListaEjecucionEntity)

    @Query("SELECT * FROM lista_ejecucion WHERE listaId = :listaId ORDER BY fecha DESC")
    fun observarPorLista(listaId: String): Flow<List<ListaEjecucionEntity>>
}
