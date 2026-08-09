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

    @Query("SELECT * FROM espacio WHERE id = :id")
    suspend fun obtenerPorId(id: String): EspacioEntity?

    /** Espacios a los que pertenece un usuario (Personal + Familia/Equipo si los hay), más viejo primero. */
    @Query(
        "SELECT e.* FROM espacio e INNER JOIN espacio_miembro m ON m.espacioId = e.id " +
            "WHERE m.usuarioId = :usuarioId ORDER BY e.fechaCreacion ASC",
    )
    fun observarEspaciosDeUsuario(usuarioId: String): Flow<List<EspacioEntity>>

    @Query("SELECT COUNT(*) FROM espacio")
    suspend fun contar(): Int

    @Query("UPDATE espacio SET nombre = :nombre WHERE id = :id")
    suspend fun actualizarNombre(id: String, nombre: String)

    /** Cascada automática a `espacio_miembro`/`reto_familiar` vía `onDelete = CASCADE`; las
     * tablas con `onDelete = RESTRICT` (actividad/meta/lista/finanzas) se limpian antes, en
     * `EspacioRepositoryImpl.eliminarEspacio`. */
    @Query("DELETE FROM espacio WHERE id = :id")
    suspend fun eliminar(id: String)
}

@Dao
interface EspacioMiembroDao {

    @Upsert
    suspend fun upsert(miembro: EspacioMiembroEntity)

    @Query("SELECT * FROM espacio_miembro WHERE espacioId = :espacioId")
    fun observarMiembros(espacioId: String): Flow<List<EspacioMiembroEntity>>

    @Query("SELECT * FROM espacio_miembro WHERE espacioId = :espacioId AND usuarioId = :usuarioId LIMIT 1")
    suspend fun obtenerMiembro(espacioId: String, usuarioId: String): EspacioMiembroEntity?
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
