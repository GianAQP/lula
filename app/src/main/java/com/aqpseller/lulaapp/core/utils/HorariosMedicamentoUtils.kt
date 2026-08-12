package com.aqpseller.lulaapp.core.utils

import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.Comida
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.MomentoRelativoComida
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
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
 * Subconjunto de `horariosCalculados` que de verdad ocurre en el primer día (`fechaInicio`) —
 * solo importa en modo por intervalo. `calcularHorariosPorIntervalo` arma "todos los horarios
 * de un día completo" dando la vuelta a la medianoche cuando `intervaloHoras` no llega justo a
 * 24h (ej. cada 8 horas desde las 14:00 → 14:00, 22:00, 06:00) — ese último horario que da la
 * vuelta es en realidad del día SIGUIENTE, no del primer día. Antes esto no se filtraba y el
 * primer día mostraba una toma de más, antes de la hora en que arrancó de verdad el tratamiento
 * (ver `Plan/08-decisiones-tecnicas.md`). Por comida no da la vuelta (no hay una única
 * "horaPrimeraDosis" de referencia), así que no hay nada que recortar.
 */
fun horariosDelPrimerDia(
    horariosCalculados: List<String>,
    modoFrecuencia: ModoFrecuenciaMedicamento,
    horaPrimeraDosis: String?,
): List<String> =
    if (modoFrecuencia == ModoFrecuenciaMedicamento.INTERVALO_HORAS && horaPrimeraDosis != null) {
        horariosCalculados.filter { it >= horaPrimeraDosis }
    } else {
        horariosCalculados
    }

/**
 * Fecha (medianoche local) del día en que cae la dosis número [cantidadDosisTotal] — cuenta las
 * que caben de verdad en el primer día (`horariosDelPrimerDia`, puede ser menos que
 * `horariosCalculados.size` completo, ver esa función) y de ahí en más un día entero por cada
 * `horariosCalculados.size` dosis. Antes contaba el primer día como si tuviera todos los
 * horarios completos, así que el tratamiento terminaba un día antes de lo que correspondía (la
 * última toma real desaparecía) — ver `Plan/08-decisiones-tecnicas.md`.
 */
fun calcularFechaFinPorCantidadDosis(
    fechaInicioMillis: Long,
    horariosCalculados: List<String>,
    cantidadDosisTotal: Int,
    modoFrecuencia: ModoFrecuenciaMedicamento,
    horaPrimeraDosis: String?,
): Long? {
    if (horariosCalculados.isEmpty() || cantidadDosisTotal <= 0) return null
    val dosisDia1 = horariosDelPrimerDia(horariosCalculados, modoFrecuencia, horaPrimeraDosis).size.coerceAtLeast(1)
    val diasAdicionales = if (cantidadDosisTotal <= dosisDia1) {
        0
    } else {
        1 + (cantidadDosisTotal - dosisDia1 - 1) / horariosCalculados.size
    }
    val diaInicio = DateTimeUtils.epochMillisToLocalDate(fechaInicioMillis)
    val diaFin = diaInicio.plus(DatePeriod(days = diasAdicionales))
    return diaFin.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

/**
 * Horarios de [detalle] que de verdad ocurren en [fecha] — punto único usado por Calendario,
 * "Tomas de hoy" y el recordatorio del día siguiente, para no repetir (y volver a desalinear,
 * como pasó con el bug del primer/último día) esta cuenta en 3 lugares distintos. Aplica, en
 * orden: fuera de rango (antes de `fechaInicio` o después de `fechaFin`) → nada; primer día →
 * `horariosDelPrimerDia`; último día (solo si el fin se calculó por "cantidad de dosis") → solo
 * hasta agotar `cantidadDosisTotal`, contando lo ya mostrado los días anteriores; cualquier otro
 * día → todos los horarios, ordenados por hora. Ver `Plan/08-decisiones-tecnicas.md`.
 */
fun horariosParaFecha(detalle: ActividadDetalle.Medicamento, fecha: LocalDate): List<String> {
    if (detalle.horariosCalculados.isEmpty()) return emptyList()
    val fechaInicioDay = DateTimeUtils.epochMillisToLocalDate(detalle.fechaInicio)
    val fechaFinDay = detalle.fechaFin?.let { DateTimeUtils.epochMillisToLocalDate(it) }
    if (fecha < fechaInicioDay || (fechaFinDay != null && fecha > fechaFinDay)) return emptyList()

    val horariosDia1 = horariosDelPrimerDia(detalle.horariosCalculados, detalle.modoFrecuencia, detalle.horaPrimeraDosis)
    val esPrimerDia = fecha == fechaInicioDay
    // Por comida el orden original SÍ importa (empareja por índice con `comidasRelacionadas`
    // en `instruccionParaHorario`) — solo por intervalo hace falta ordenar por hora, porque ahí
    // el arreglo puede traer un horario que dio la vuelta a la medianoche al final de la lista.
    val horariosDeUnDiaCompleto = if (detalle.modoFrecuencia == ModoFrecuenciaMedicamento.INTERVALO_HORAS) {
        detalle.horariosCalculados.sorted()
    } else {
        detalle.horariosCalculados
    }

    if (detalle.cantidadDosisTotal == null) {
        return if (esPrimerDia) horariosDia1 else horariosDeUnDiaCompleto
    }
    if (esPrimerDia && fecha == fechaFinDay) {
        // Todo el tratamiento cabe en el primer día.
        return horariosDia1.take(detalle.cantidadDosisTotal)
    }
    if (esPrimerDia) return horariosDia1
    if (fechaFinDay != null && fecha == fechaFinDay) {
        val diasIntermedios = (fechaFinDay.toEpochDays() - fechaInicioDay.toEpochDays() - 1).coerceAtLeast(0)
        val dosisConsumidasAntes = horariosDia1.size + diasIntermedios * detalle.horariosCalculados.size
        val dosisDelUltimoDia = (detalle.cantidadDosisTotal - dosisConsumidasAntes).coerceIn(0, detalle.horariosCalculados.size)
        return horariosDeUnDiaCompleto.take(dosisDelUltimoDia)
    }
    return horariosDeUnDiaCompleto
}

private fun parsearHoraOMedianoche(hora: String): Pair<Int, Int>? {
    val partes = hora.split(":")
    val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
    val m = partes.getOrNull(1)?.toIntOrNull() ?: return null
    return h to m
}
