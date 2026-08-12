package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.EntradaDiarioEntity
import com.aqpseller.lulaapp.data.local.entity.HistorialCambiosEntity
import com.aqpseller.lulaapp.data.local.entity.MetaActividadCrossRef
import com.aqpseller.lulaapp.data.local.entity.MetaEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarParticipanteEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarRegistroEntity
import com.aqpseller.lulaapp.data.local.entity.SolicitudCompartirEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Upsert
    suspend fun upsert(meta: MetaEntity)

    @Upsert
    suspend fun upsertVinculo(vinculo: MetaActividadCrossRef)

    @Query("SELECT * FROM meta WHERE espacioId = :espacioId ORDER BY fechaLimite IS NULL, fechaLimite ASC")
    fun observarPorEspacio(espacioId: String): Flow<List<MetaEntity>>

    @Query("SELECT * FROM meta WHERE id = :id")
    suspend fun obtenerPorId(id: String): MetaEntity?

    @Query("SELECT actividadId FROM meta_actividad_cross_ref WHERE metaId = :metaId LIMIT 1")
    suspend fun obtenerActividadVinculada(metaId: String): String?

    @Query("UPDATE meta SET valorActual = :valorActual WHERE id = :id")
    suspend fun actualizarValorActual(id: String, valorActual: Double)

    @Query("UPDATE meta SET fechaLimite = :fechaLimite WHERE id = :id")
    suspend fun actualizarFechaLimite(id: String, fechaLimite: Long?)

    @Query("DELETE FROM meta WHERE id = :id")
    suspend fun eliminar(id: String)

    /** Para eliminar un espacio completo (ver `EspacioRepositoryImpl.eliminarEspacio`). */
    @Query("DELETE FROM meta WHERE espacioId = :espacioId")
    suspend fun eliminarPorEspacio(espacioId: String)
}

@Dao
interface EntradaDiarioDao {
    @Upsert
    suspend fun upsert(entrada: EntradaDiarioEntity)

    /** Orden por `fecha` (el día que el usuario eligió para la entrada), no por cuándo se creó
     * la fila — si se cargan entradas de días salteados o fuera de orden, igual quedan
     * ordenadas por su fecha real. */
    @Query("SELECT * FROM entrada_diario WHERE espacioId = :espacioId ORDER BY fecha DESC, id DESC")
    fun observarPorEspacio(espacioId: String): Flow<List<EntradaDiarioEntity>>

    @Query("SELECT * FROM entrada_diario WHERE id = :id")
    suspend fun obtenerPorId(id: String): EntradaDiarioEntity?

    @Query("DELETE FROM entrada_diario WHERE id = :id")
    suspend fun eliminar(id: String)
}

@Dao
interface RetoFamiliarDao {
    @Upsert
    suspend fun upsert(reto: RetoFamiliarEntity)

    @Upsert
    suspend fun upsertParticipante(participante: RetoFamiliarParticipanteEntity)

    @Query("SELECT * FROM reto_familiar WHERE espacioId = :espacioId")
    fun observarPorEspacio(espacioId: String): Flow<List<RetoFamiliarEntity>>

    @Query("SELECT * FROM reto_familiar_participante WHERE retoId = :retoId")
    suspend fun obtenerParticipantes(retoId: String): List<RetoFamiliarParticipanteEntity>

    @Upsert
    suspend fun upsertRegistro(registro: RetoFamiliarRegistroEntity)

    @Query("SELECT * FROM reto_familiar_registro WHERE retoId = :retoId AND fecha = :fecha")
    suspend fun obtenerRegistrosDeHoy(retoId: String, fecha: Long): List<RetoFamiliarRegistroEntity>

    @Query("SELECT * FROM reto_familiar_registro WHERE retoId = :retoId AND usuarioId = :usuarioId AND fecha = :fecha LIMIT 1")
    suspend fun obtenerRegistro(retoId: String, usuarioId: String, fecha: Long): RetoFamiliarRegistroEntity?
}

@Dao
interface SolicitudCompartirDao {
    @Upsert
    suspend fun upsert(solicitud: SolicitudCompartirEntity)

    @Query("SELECT * FROM solicitud_compartir WHERE id = :id")
    suspend fun obtenerPorId(id: String): SolicitudCompartirEntity?

    @Query("SELECT * FROM solicitud_compartir WHERE de = :usuarioId ORDER BY fechaSolicitud DESC")
    fun observarEnviadasPor(usuarioId: String): Flow<List<SolicitudCompartirEntity>>

    /** Para cuando exista aceptación real entre cuentas — hoy siempre vacío en un solo dispositivo. */
    @Query("SELECT * FROM solicitud_compartir WHERE para = :usuarioId AND estado = 'PENDIENTE'")
    fun observarPendientesPara(usuarioId: String): Flow<List<SolicitudCompartirEntity>>

    @Query("DELETE FROM solicitud_compartir WHERE id = :id")
    suspend fun eliminar(id: String)
}

@Dao
interface HistorialCambiosDao {
    @Upsert
    suspend fun upsert(registro: HistorialCambiosEntity)

    @Query("SELECT * FROM historial_cambios WHERE entidad = :entidad AND entidadId = :entidadId ORDER BY timestamp DESC")
    fun observarPorEntidad(entidad: String, entidadId: String): Flow<List<HistorialCambiosEntity>>
}
