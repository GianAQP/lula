package com.aqpseller.lulaapp.core.utils

import com.aqpseller.lulaapp.domain.model.Recurrencia
import kotlinx.datetime.LocalDate

/**
 * ¿Una Fecha importante con `recurrencia` ocurre en [fecha]? Misma lógica de
 * "próxima ocurrencia" que usa `RecordatorioScheduler` para reprogramar la alarma, pero como
 * comprobación puntual — la reutiliza el Calendario para ubicar cada Fecha importante en el
 * día que corresponde, sin mutar nunca `fechaBase`.
 */
fun ocurreEnFecha(fechaBaseMillis: Long, recurrencia: Recurrencia, fecha: LocalDate): Boolean {
    val base = DateTimeUtils.epochMillisToLocalDate(fechaBaseMillis)
    if (fecha < base) return false
    return when (recurrencia) {
        Recurrencia.UNICA -> fecha == base
        Recurrencia.SEMANAL -> (fecha.toEpochDays() - base.toEpochDays()) % 7 == 0
        Recurrencia.ANUAL -> fecha.monthNumber == base.monthNumber && fecha.dayOfMonth == base.dayOfMonth
    }
}
