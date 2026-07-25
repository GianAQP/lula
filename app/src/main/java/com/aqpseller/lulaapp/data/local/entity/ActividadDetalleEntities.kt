package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    ],
    indices = [Index("fechaLimite")],
)
data class TareaDetalleEntity(
    @PrimaryKey val actividadId: String,
    val fechaLimite: Long?,
    val prioridad: Int?,
    val importante: Boolean,
    val urgente: Boolean,
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
)

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
    val recordatorioAnticipacion: String,
)

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
