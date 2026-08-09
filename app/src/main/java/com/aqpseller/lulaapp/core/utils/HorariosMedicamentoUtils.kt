package com.aqpseller.lulaapp.core.utils

import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.Comida
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.MomentoRelativoComida
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

/** Horarios de un día completo, cada `intervaloHoras` desde `horaPrimeraDosis` (formato "HH:mm"). */
fun calcularHorariosPorIntervalo(horaPrimeraDosis: String, intervaloHoras: Int): List<String> {
    val (hora, minuto) = parsearHoraOMedianoche(horaPrimeraDosis) ?: return emptyList()
    if (intervaloHoras <= 0) return listOf(horaPrimeraDosis)
    val dosisPorDia = (24 / intervaloHoras).coerceAtLeast(1)
    val minutoInicial = hora * 60 + minuto
    return (0 until dosisPorDia).map { i ->
        val totalMin = (minutoInicial + i * intervaloHoras * 60).mod(24 * 60)
        "%02d:%02d".format(totalMin / 60, totalMin % 60)
    }
}

/**
 * Un horario por cada `ComidaRelacionada`, **en el mismo orden** — así la pantalla puede
 * mostrar "después del almuerzo" junto a la hora sin guardar un mapa aparte. Si el usuario no
 * guardó todavía la hora de esa comida (`Usuario.horaDesayuno`/etc.), esa comida se omite.
 */
fun calcularHorariosPorComida(
    comidasRelacionadas: List<ComidaRelacionada>,
    horaDesayuno: String?,
    horaAlmuerzo: String?,
    horaCena: String?,
): List<String> = comidasRelacionadas.mapNotNull { relacion ->
    when (relacion.comida) {
        Comida.DESAYUNO -> horaDesayuno
        Comida.ALMUERZO -> horaAlmuerzo
        Comida.CENA -> horaCena
    }
}

/** "después del almuerzo" — la instrucción original, para mostrar siempre junto a la hora calculada. */
fun instruccionComida(relacion: ComidaRelacionada): String {
    val comida = when (relacion.comida) {
        Comida.DESAYUNO -> "el desayuno"
        Comida.ALMUERZO -> "el almuerzo"
        Comida.CENA -> "la cena"
    }
    val momento = if (relacion.momento == MomentoRelativoComida.ANTES) "Antes de" else "Después de"
    return "$momento $comida"
}

/**
 * Texto de instrucción para el horario en la posición [index] de `horariosCalculados` — igual
 * para todas las tomas si es por intervalo, o específico de esa comida si es "según las
 * comidas" (mismo orden que `comidasRelacionadas`, ver `calcularHorariosPorComida`).
 */
fun instruccionParaHorario(detalle: ActividadDetalle.Medicamento, index: Int): String =
    if (detalle.modoFrecuencia == ModoFrecuenciaMedicamento.RELACION_COMIDA) {
        detalle.comidasRelacionadas.getOrNull(index)?.let { instruccionComida(it) } ?: "Según indicación"
    } else {
        "Cada ${detalle.intervaloHoras ?: 0} horas"
    }

/**
 * Fecha (medianoche local) del día en que cae la dosis número [cantidadDosisTotal] — cuenta
 * desde `horariosCalculados[0]` en `fechaInicio` y avanza uno a uno, ciclando por la lista cada
 * día (`horariosCalculados` es siempre "las tomas de un día completo", ver
 * `calcularHorariosPorIntervalo`/`calcularHorariosPorComida`). Así "pongo cuántas dosis recetó
 * el médico" calcula la fecha de fin sola, sin tener que contar días a mano — ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
fun calcularFechaFinPorCantidadDosis(fechaInicioMillis: Long, horariosCalculados: List<String>, cantidadDosisTotal: Int): Long? {
    if (horariosCalculados.isEmpty() || cantidadDosisTotal <= 0) return null
    val diasCompletos = (cantidadDosisTotal - 1) / horariosCalculados.size
    val diaInicio = DateTimeUtils.epochMillisToLocalDate(fechaInicioMillis)
    val diaFin = diaInicio.plus(DatePeriod(days = diasCompletos))
    return diaFin.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

/**
 * Índice (0-based, dentro de `horariosCalculados`) de la última dosis permitida en el día en
 * que termina el tratamiento — las tomas después de ese índice, ese mismo día, no cuentan
 * (evita el error de "6 tomas" cuando el médico recetó 4, ver `08-decisiones-tecnicas.md`).
 */
fun indiceUltimaDosisEnDiaFinal(horariosCalculados: List<String>, cantidadDosisTotal: Int): Int =
    (cantidadDosisTotal - 1).mod(horariosCalculados.size)

private fun parsearHoraOMedianoche(hora: String): Pair<Int, Int>? {
    val partes = hora.split(":")
    val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
    val m = partes.getOrNull(1)?.toIntOrNull() ?: return null
    return h to m
}
