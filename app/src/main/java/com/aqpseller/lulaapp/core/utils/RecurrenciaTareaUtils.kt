package com.aqpseller.lulaapp.core.utils

import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

/** Próxima fecha límite de una Tarea recurrente, contando desde su fecha actual. */
fun siguienteFechaTareaRecurrente(fechaBaseEpochMillis: Long, recurrencia: RecurrenciaTarea): Long {
    val fecha = DateTimeUtils.epochMillisToLocalDate(fechaBaseEpochMillis)
    val siguiente = when (recurrencia) {
        RecurrenciaTarea.SIN_REPETIR -> fecha
        RecurrenciaTarea.DIARIA -> fecha.plus(DatePeriod(days = 1))
        RecurrenciaTarea.SEMANAL -> fecha.plus(DatePeriod(days = 7))
        RecurrenciaTarea.QUINCENAL -> fecha.plus(DatePeriod(days = 14))
        RecurrenciaTarea.MENSUAL -> fecha.plus(DatePeriod(months = 1))
        RecurrenciaTarea.BIMESTRAL -> fecha.plus(DatePeriod(months = 2))
        RecurrenciaTarea.TRIMESTRAL -> fecha.plus(DatePeriod(months = 3))
        RecurrenciaTarea.ANUAL -> fecha.plus(DatePeriod(years = 1))
    }
    return siguiente.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
