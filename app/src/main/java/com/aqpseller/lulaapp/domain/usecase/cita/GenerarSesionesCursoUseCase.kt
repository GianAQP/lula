package com.aqpseller.lulaapp.domain.usecase.cita

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionCita
import javax.inject.Inject

/**
 * Calcula las próximas sesiones de un curso de Cita (radioterapia, masajes, etc.) a partir de
 * `diasSemana` — nunca toca sesiones que ya existen, solo agrega las que faltan desde donde se
 * quedó (o desde `fechaInicioCurso` si el curso es nuevo). Un curso con `cantidadSesionesTotal`
 * se detiene ahí; uno abierto (masajes sin cantidad fija) genera un horizonte de
 * [HORIZONTE_DIAS_ABIERTO] días, que `ExtenderSesionesCursoSiHaceFaltaUseCase` vuelve a llamar
 * cuando hace falta. Ver `Plan/08-decisiones-tecnicas.md`.
 */
class GenerarSesionesCursoUseCase @Inject constructor() {

    companion object {
        const val HORIZONTE_DIAS_ABIERTO = 90
    }

    operator fun invoke(
        actividadId: String,
        detalle: ActividadDetalle.Cita,
        sesionesExistentes: List<SesionCita>,
    ): List<SesionCita> {
        if (!detalle.esCurso || detalle.diasSemana.isEmpty()) return emptyList()
        val horaSesion = detalle.horaSesion ?: "09:00"
        val ultimoNumero = sesionesExistentes.maxOfOrNull { it.numeroSesion } ?: 0
        val cantidadTotal = detalle.cantidadSesionesTotal
        if (cantidadTotal != null && ultimoNumero >= cantidadTotal) return emptyList()

        val epochDayInicio = if (sesionesExistentes.isEmpty()) {
            val inicio = detalle.fechaInicioCurso ?: DateTimeUtils.inicioDeHoyEpochMillis()
            DateTimeUtils.epochMillisToLocalDate(inicio).toEpochDays().toLong()
        } else {
            sesionesExistentes.maxOf { it.fechaOriginal } + 1
        }
        val limiteEpochDay = epochDayInicio + HORIZONTE_DIAS_ABIERTO

        val nuevas = mutableListOf<SesionCita>()
        var epochDay = epochDayInicio
        var numero = ultimoNumero
        // Tope de seguridad: si el patrón de días quedara vacío o mal configurado, nunca debe
        // convertirse en un bucle infinito — 10 años de margen es más que suficiente.
        val topeSeguridad = epochDayInicio + 3650
        while (epochDay <= topeSeguridad) {
            if (cantidadTotal != null && numero >= cantidadTotal) break
            if (cantidadTotal == null && epochDay > limiteEpochDay) break
            val fecha = DateTimeUtils.epochDaysToLocalDate(epochDay)
            if (DateTimeUtils.numeroDiaIso(fecha) in detalle.diasSemana) {
                numero += 1
                nuevas += SesionCita(
                    id = IdGenerator.newId(),
                    actividadId = actividadId,
                    numeroSesion = numero,
                    fecha = epochDay,
                    fechaOriginal = epochDay,
                    horario = horaSesion,
                    estado = EstadoActividad.SIN_CONFIRMAR,
                )
            }
            epochDay += 1
        }
        return nuevas
    }
}
