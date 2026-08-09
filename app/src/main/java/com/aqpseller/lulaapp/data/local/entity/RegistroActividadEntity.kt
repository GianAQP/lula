package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Registro de cumplimiento por día de una `Actividad` recurrente (hábito). Necesario porque
 * `ActividadEntity.estado` es un solo valor "actual" — no alcanza para saber si un hábito se
 * cumplió hace 3 días o calcular su racha/tracker semanal. Las tareas (no recurrentes) siguen
 * usando `ActividadEntity.estado` directamente, ver `Plan/08-decisiones-tecnicas.md`.
 */
@Serializable
@Entity(
    tableName = "registro_actividad",
    foreignKeys = [
        ForeignKey(
            entity = ActividadEntity::class,
            parentColumns = ["id"],
            childColumns = ["actividadId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["actividadId", "fecha"], unique = true), Index("fecha")],
)
data class RegistroActividadEntity(
    @PrimaryKey val id: String,
    val actividadId: String,
    val fecha: Long,
    val estado: String,
)
