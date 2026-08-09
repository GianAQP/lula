package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "habito_detalle",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HabitoDetalleEntity(
    @PrimaryKey val actividadId: String,
    val momentoDelDia: String,
    val frecuencia: String,
    val diasEspecificosJson: String?,
    val duracionInicialMin: Int?,
    val duracionObjetivoMin: Int?,
    val incrementoMin: Int?,
    val frecuenciaRevisionDias: Int?,
    /** Formato "HH:mm", null = sin recordatorio. Ver `core/notifications/`. */
    val horaRecordatorio: String?,
    val nivelRecordatorio: String,
    val duracionActualMin: Int?,
    val proximaRevisionEpochDay: Long?,
)

@Entity(
    tableName = "tarea_detalle",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Sin CASCADE acá a propósito: si se elimina el Medicamento/Cita vinculado, la Tarea no
        // debe desaparecer con él, solo perder el vínculo.
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadVinculadaId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("fechaLimite"), Index("actividadVinculadaId")],
)
data class TareaDetalleEntity(
    @PrimaryKey val actividadId: String,
    val fechaLimite: Long?,
    val prioridad: Int?,
    val importante: Boolean,
    val urgente: Boolean,
    /** Formato "HH:mm", solo tiene efecto si hay `fechaLimite`. Ver `core/notifications/`. */
    val horaRecordatorio: String?,
    val nivelRecordatorio: String,
    val recurrencia: String,
    val actividadVinculadaId: String? = null,
)

@Entity(
    tableName = "rutina_detalle",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RutinaDetalleEntity(
    @PrimaryKey val actividadId: String,
    val actividadesIncluidasJson: String,
    val momentoDelDia: String,
)

@Serializable
@Entity(
    tableName = "medicamento_detalle",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fechaInicio"), Index("fechaFin")],
)
data class MedicamentoDetalleEntity(
    @PrimaryKey val actividadId: String,
    val nombreMedicamento: String,
    val dosis: String,
    val modoFrecuencia: String,
    val intervaloHoras: Int?,
    val horaPrimeraDosis: String?,
    val horariosCalculadosJson: String?,
    val comidasRelacionadasJson: String?,
    val fechaInicio: Long,
    val fechaFin: Long?,
    val cantidadDosisTotal: Int?,
    val nivelRecordatorio: String,
    val recordatorioPersistente: Boolean,
    val intervaloPersistenciaMin: Int?,
)

@Serializable
@Entity(
    tableName = "cita_detalle",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fechaHora")],
)
data class CitaDetalleEntity(
    @PrimaryKey val actividadId: String,
    val lugar: String?,
    val motivo: String?,
    val fechaHora: Long,
    val recordatoriosJson: String?,
    val nivelRecordatorio: String,
    val esCurso: Boolean = false,
    val diasSemanaJson: String? = null,
    val horaSesion: String? = null,
    val fechaInicioCurso: Long? = null,
    val cantidadSesionesTotal: Int? = null,
)

/** Una toma de Medicamento — a diferencia de Hábito, hay varias por día, una fila por horario. */
@Serializable
@Entity(
    tableName = "toma_medicamento",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["actividadId", "fecha", "horario"], unique = true)],
)
data class TomaMedicamentoEntity(
    @PrimaryKey val id: String,
    val actividadId: String,
    val fecha: Long,
    val horario: String,
    val estado: String,
)

/** Una sesión de una Cita de curso — una fila por ocurrencia, mismo principio que `TomaMedicamentoEntity`. */
@Serializable
@Entity(
    tableName = "sesion_cita",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["actividadId", "numeroSesion"], unique = true), Index("fecha")],
)
data class SesionCitaEntity(
    @PrimaryKey val id: String,
    val actividadId: String,
    val numeroSesion: Int,
    val fecha: Long,
    val fechaOriginal: Long,
    val horario: String,
    val estado: String,
)

@Serializable
@Entity(
    tableName = "fecha_importante_detalle",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fechaBase")],
)
data class FechaImportanteDetalleEntity(
    @PrimaryKey val actividadId: String,
    val recurrencia: String,
    val fechaBase: Long,
    val horaNotificacion: String,
    val anticipacion: String,
    val tipoAviso: String,
)
