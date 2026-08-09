package com.aqpseller.lulaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aqpseller.lulaapp.data.local.entity.CitaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.FechaImportanteDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.HabitoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.MedicamentoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.RutinaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.SesionCitaEntity
import com.aqpseller.lulaapp.data.local.entity.TareaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.TomaMedicamentoEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM tarea_detalle WHERE actividadVinculadaId = :actividadVinculadaId")
    suspend fun obtenerPorActividadVinculadaId(actividadVinculadaId: String): List<TareaDetalleEntity>

    @Query("SELECT * FROM tarea_detalle WHERE actividadVinculadaId IS NOT NULL")
    suspend fun obtenerConVinculo(): List<TareaDetalleEntity>
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

    @Query("SELECT * FROM medicamento_detalle WHERE actividadId IN (:actividadIds)")
    suspend fun obtenerPorActividadIds(actividadIds: List<String>): List<MedicamentoDetalleEntity>

    @Query("SELECT * FROM medicamento_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): MedicamentoDetalleEntity?
}

@Dao
interface CitaDetalleDao {
    @Upsert
    suspend fun upsert(detalle: CitaDetalleEntity)

    @Query("SELECT * FROM cita_detalle WHERE actividadId IN (:actividadIds)")
    suspend fun obtenerPorActividadIds(actividadIds: List<String>): List<CitaDetalleEntity>

    @Query("SELECT * FROM cita_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): CitaDetalleEntity?
}

@Dao
interface TomaMedicamentoDao {
    @Upsert
    suspend fun upsert(toma: TomaMedicamentoEntity)

    @Query("SELECT * FROM toma_medicamento WHERE actividadId = :actividadId AND fecha = :fecha")
    suspend fun obtenerPorActividadIdYFecha(actividadId: String, fecha: Long): List<TomaMedicamentoEntity>

    @Query("SELECT * FROM toma_medicamento WHERE actividadId IN (:actividadIds) AND fecha = :fecha")
    suspend fun obtenerPorActividadIdsYFecha(actividadIds: List<String>, fecha: Long): List<TomaMedicamentoEntity>

    @Query("SELECT * FROM toma_medicamento WHERE fecha = :fecha")
    fun observarPorFecha(fecha: Long): Flow<List<TomaMedicamentoEntity>>

    @Query("SELECT * FROM toma_medicamento WHERE actividadId = :actividadId AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtenerPorActividadIdYRango(actividadId: String, desde: Long, hasta: Long): List<TomaMedicamentoEntity>
}

@Dao
interface SesionCitaDao {
    @Upsert
    suspend fun upsert(sesion: SesionCitaEntity)

    @Query("SELECT * FROM sesion_cita WHERE actividadId = :actividadId ORDER BY numeroSesion")
    suspend fun obtenerPorActividadId(actividadId: String): List<SesionCitaEntity>

    @Query("SELECT * FROM sesion_cita WHERE actividadId IN (:actividadIds) AND fecha BETWEEN :desde AND :hasta")
    suspend fun obtenerPorActividadIdsYRango(actividadIds: List<String>, desde: Long, hasta: Long): List<SesionCitaEntity>

    @Query("SELECT * FROM sesion_cita WHERE actividadId = :actividadId AND numeroSesion = :numeroSesion")
    suspend fun obtenerPorNumeroSesion(actividadId: String, numeroSesion: Int): SesionCitaEntity?

    @Query("SELECT * FROM sesion_cita WHERE actividadId IN (:actividadIds) AND fecha = :fecha")
    suspend fun obtenerPorActividadIdsYFecha(actividadIds: List<String>, fecha: Long): List<SesionCitaEntity>
}

@Dao
interface FechaImportanteDetalleDao {
    @Upsert
    suspend fun upsert(detalle: FechaImportanteDetalleEntity)

    @Query("SELECT * FROM fecha_importante_detalle WHERE actividadId = :actividadId")
    suspend fun obtenerPorActividadId(actividadId: String): FechaImportanteDetalleEntity?

    @Query("SELECT * FROM fecha_importante_detalle WHERE actividadId IN (:actividadIds)")
    suspend fun obtenerPorActividadIds(actividadIds: List<String>): List<FechaImportanteDetalleEntity>
}
