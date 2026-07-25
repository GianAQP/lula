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
import com.aqpseller.lulaapp.data.local.entity.SolicitudCompartirEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    @Upsert
    suspend fun upsert(meta: MetaEntity)

    @Upsert
    suspend fun upsertVinculo(vinculo: MetaActividadCrossRef)

    @Query("SELECT * FROM meta WHERE espacioId = :espacioId")
    fun observarPorEspacio(espacioId: String): Flow<List<MetaEntity>>
}

@Dao
interface EntradaDiarioDao {
    @Upsert
    suspend fun upsert(entrada: EntradaDiarioEntity)

    @Query("SELECT * FROM entrada_diario ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<EntradaDiarioEntity>>
}

@Dao
interface RetoFamiliarDao {
    @Upsert
    suspend fun upsert(reto: RetoFamiliarEntity)

    @Upsert
    suspend fun upsertParticipante(participante: RetoFamiliarParticipanteEntity)

    @Query("SELECT * FROM reto_familiar WHERE espacioId = :espacioId")
    fun observarPorEspacio(espacioId: String): Flow<List<RetoFamiliarEntity>>
}

@Dao
interface SolicitudCompartirDao {
    @Upsert
    suspend fun upsert(solicitud: SolicitudCompartirEntity)

    @Query("SELECT * FROM solicitud_compartir WHERE para = :usuarioId AND estado = 'pendiente'")
    fun observarPendientesPara(usuarioId: String): Flow<List<SolicitudCompartirEntity>>
}

@Dao
interface HistorialCambiosDao {
    @Upsert
    suspend fun upsert(registro: HistorialCambiosEntity)

    @Query("SELECT * FROM historial_cambios WHERE entidad = :entidad AND entidadId = :entidadId ORDER BY timestamp DESC")
    fun observarPorEntidad(entidad: String, entidadId: String): Flow<List<HistorialCambiosEntity>>
}
