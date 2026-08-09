package com.aqpseller.lulaapp.core.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object DateTimeUtils {

    fun hoy(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    fun ahoraEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun epochMillisToLocalDate(epochMillis: Long): LocalDate =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun inicioDeHoyEpochMillis(): Long =
        hoy().atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    fun finDeHoyEpochMillis(): Long = inicioDeHoyEpochMillis() + 86_400_000L - 1

    fun inicioDeMesEpochMillis(fecha: LocalDate = hoy()): Long =
        LocalDate(fecha.year, fecha.month, 1).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    fun finDeMesEpochMillis(fecha: LocalDate = hoy()): Long {
        val inicioSiguienteMes = LocalDate(fecha.year, fecha.month, 1).plus(DatePeriod(months = 1))
        return inicioSiguienteMes.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds() - 1
    }

    fun epochDaysToLocalDate(epochDays: Long): LocalDate = LocalDate.fromEpochDays(epochDays.toInt())

    /** Medianoche local (epoch millis) de un epoch day — para combinar con una hora vía `combinarFechaYHora`. */
    fun epochDiasAEpochMillis(epochDays: Long): Long =
        epochDaysToLocalDate(epochDays).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    /**
     * Número de día ISO (1=lunes..7=domingo) calculado por aritmética de módulo sobre
     * epoch day, sin usar `java.time.DayOfWeek` — el proyecto tiene `minSdk = 24` sin core
     * library desugaring habilitado, y `LocalDate.dayOfWeek` de kotlinx-datetime delega en
     * `java.time.DayOfWeek` (requiere API 26+ sin desugaring). El epoch day 0 (1970-01-01) fue
     * jueves (ISO 4), de ahí el offset `+3`.
     */
    private fun numeroDiaIso(epochDays: Long): Int = (((epochDays % 7) + 7 + 3) % 7 + 1).toInt()

    /** Número de día ISO (1=lunes..7=domingo) de hoy — para comparar contra un día configurado. */
    fun numeroDiaIsoDeHoy(): Int = numeroDiaIso(hoy().toEpochDays().toLong())

    /** Número de día ISO (1=lunes..7=domingo) de una fecha cualquiera — ej. para saber si cae
     * dentro del patrón de días de un curso de Cita. */
    fun numeroDiaIso(fecha: LocalDate): Int = numeroDiaIso(fecha.toEpochDays().toLong())

    private val NOMBRES_DIA_SEMANA = listOf(
        "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo",
    )

    /** Nombre completo del día ISO (1=lunes..7=domingo) — para textos como "se activa el domingo". */
    fun nombreDiaIso(numeroDiaIso: Int): String = NOMBRES_DIA_SEMANA[numeroDiaIso - 1]

    /** Lunes (epoch day) de la semana ISO que contiene [fecha]. */
    fun inicioDeSemanaEpochDias(fecha: LocalDate = hoy()): Long {
        val epochDays = fecha.toEpochDays().toLong()
        return epochDays - (numeroDiaIso(epochDays) - 1)
    }

    /** Domingo (epoch day) de la semana ISO que contiene [fecha]. */
    fun finDeSemanaEpochDias(fecha: LocalDate = hoy()): Long = inicioDeSemanaEpochDias(fecha) + 6

    /** Identificador único y ordenable de semana: fecha ISO de su lunes (ej. "2026-07-20"). */
    fun claveSemana(fecha: LocalDate = hoy()): String = epochDaysToLocalDate(inicioDeSemanaEpochDias(fecha)).toString()

    /** Se fija al día 1 antes de sumar meses para no romper con "31 de marzo + 1 mes". */
    fun agregarMeses(fecha: LocalDate, cantidad: Int): LocalDate =
        LocalDate(fecha.year, fecha.monthNumber, 1).plus(DatePeriod(months = cantidad))

    fun secuenciaFechas(desde: LocalDate, hasta: LocalDate): List<LocalDate> =
        generateSequence(desde) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= hasta }.toList()

    /** Rango de fechas de la grilla de 7 columnas del mes de [fecha] — semanas parciales incluidas. */
    fun rangoGrillaMes(fecha: LocalDate): Pair<LocalDate, LocalDate> {
        val primerDiaDelMes = LocalDate(fecha.year, fecha.monthNumber, 1)
        val ultimoDiaDelMes = primerDiaDelMes.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val inicioGrid = epochDaysToLocalDate(inicioDeSemanaEpochDias(primerDiaDelMes))
        val finGrid = epochDaysToLocalDate(finDeSemanaEpochDias(ultimoDiaDelMes))
        return inicioGrid to finGrid
    }

    /** Medianoche local de [fecha] en epoch millis — inverso de `epochMillisToLocalDate`. */
    fun localDateAEpochMillis(fecha: LocalDate): Long = fecha.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    private val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    fun formatearFechaLarga(fecha: LocalDate): String = "${fecha.dayOfMonth} de ${MESES[fecha.monthNumber - 1]}"

    /** "Julio 2026" — para el título de la vista de Mes del Calendario. */
    fun formatearMesYAnio(fecha: LocalDate): String =
        MESES[fecha.monthNumber - 1].replaceFirstChar { it.uppercase() } + " ${fecha.year}"

    /**
     * El `DatePicker` de Material3 trabaja en millis UTC (medianoche UTC del día elegido) —
     * estas dos funciones convierten entre esa representación y la que usa el resto de la
     * app (medianoche en la zona horaria local, ver `inicioDeHoyEpochMillis`).
     */
    fun utcMillisAInicioDeDiaLocal(utcMillis: Long): Long {
        val fecha = Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date
        return fecha.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }

    fun inicioDeDiaLocalAUtcMillis(inicioDeDiaLocalMillis: Long): Long =
        epochMillisToLocalDate(inicioDeDiaLocalMillis).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    /** [fechaEpochMillis] (cualquier hora de ese día) + [hora] "HH:mm" → millis exactos de ese momento. */
    fun combinarFechaYHora(fechaEpochMillis: Long, hora: String): Long {
        val fecha = epochMillisToLocalDate(fechaEpochMillis)
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: 0
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
        return LocalDateTime(fecha.year, fecha.monthNumber, fecha.dayOfMonth, h, m)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }

    /** Medianoche local del mismo día que [epochMillis] — para separar la parte "fecha" de un instante con hora. */
    fun inicioDeDiaEpochMillis(epochMillis: Long): Long =
        epochMillisToLocalDate(epochMillis).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    /** Formato "HH:mm" de la hora local de [epochMillis]. */
    fun horaHHmm(epochMillis: Long): String {
        val fechaHora = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        return "%02d:%02d".format(fechaHora.hour, fechaHora.minute)
    }

    /** Formato "aaaa-mm-dd", para listas donde hace falta la fecha exacta de un vistazo. */
    fun formatearFechaCorta(fecha: LocalDate): String = "%04d-%02d-%02d".format(fecha.year, fecha.monthNumber, fecha.dayOfMonth)

    private val LETRAS_DIA_SEMANA = listOf("L", "M", "m", "J", "V", "S", "D")

    /** Inicial del día de la semana de [epochMillis]: L, M, m (miércoles), J, V, S, D. */
    fun letraDiaSemana(epochMillis: Long): String {
        val epochDays = epochMillisToLocalDate(epochMillis).toEpochDays().toLong()
        return LETRAS_DIA_SEMANA[numeroDiaIso(epochDays) - 1]
    }
}
