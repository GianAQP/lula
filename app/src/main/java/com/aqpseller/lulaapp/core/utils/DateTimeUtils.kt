package com.aqpseller.lulaapp.core.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

object DateTimeUtils {

    fun hoy(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    fun ahoraEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun inicioDeHoyEpochMillis(): Long =
        hoy().atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    fun finDeHoyEpochMillis(): Long = inicioDeHoyEpochMillis() + 86_400_000L - 1
}
