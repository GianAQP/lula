package com.aqpseller.lulaapp.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Solo convierte listas que se leen siempre como bloque completo tras cargar la fila
 * (nunca se filtran en SQL) — ver `Plan/08-decisiones-tecnicas.md`. Los enums se guardan
 * como String directamente en las entidades y se mapean en la capa de repositorio.
 */
class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String? =
        list?.let { Json.encodeToString(it) }

    @TypeConverter
    fun toStringList(json: String?): List<String>? =
        json?.let { Json.decodeFromString(it) }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? =
        list?.let { Json.encodeToString(it) }

    @TypeConverter
    fun toIntList(json: String?): List<Int>? =
        json?.let { Json.decodeFromString(it) }
}
