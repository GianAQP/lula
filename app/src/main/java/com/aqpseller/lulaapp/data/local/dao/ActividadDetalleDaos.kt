package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.CitaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.FechaImportanteDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.HabitoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.MedicamentoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.RutinaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.TareaDetalleEntity

@Dao
interface HabitoDetalleDao {
    @Upsert
    suspend fun upsert(detalle: HabitoDetalleEntity)

    @Query("SELECT * FROM habito_detalle WHERE actividadId IN (:actividadIds)")
    suspend fun obtenerPorActividadIds(actividadIds: List<String>): List<HabitoDetalleEntity>

    @Query("SELECT * FROM habito_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): HabitoDetalleEntity?
}

@Dao
interface TareaDetalleDao {
    @Upsert
    suspend fun upsert(detalle: TareaDetalleEntity)

    @Query("SELECT * FROM tarea_detalle WHERE actividadId IN (:actividadIds)")
    suspend fun obtenerPorActividadIds(actividadIds: List<String>): List<TareaDetalleEntity>

    @Query("SELECT * FROM tarea_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): TareaDetalleEntity?
}

@Dao
interface RutinaDetalleDao {
    @Upsert
    suspend fun upsert(detalle: RutinaDetalleEntity)

    @Query("SELECT * FROM rutina_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): RutinaDetalleEntity?
}

@Dao
interface MedicamentoDetalleDao {
    @Upsert
    suspend fun upsert(detalle: MedicamentoDetalleEntity)

    @Query("SELECT * FROM medicamento_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): MedicamentoDetalleEntity?
}

@Dao
interface CitaDetalleDao {
    @Upsert
    suspend fun upsert(detalle: CitaDetalleEntity)

    @Query("SELECT * FROM cita_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): CitaDetalleEntity?
}

@Dao
interface FechaImportanteDetalleDao {
    @Upsert
    suspend fun upsert(detalle: FechaImportanteDetalleEntity)

    @Query("SELECT * FROM fecha_importante_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): FechaImportanteDetalleEntity?
}
