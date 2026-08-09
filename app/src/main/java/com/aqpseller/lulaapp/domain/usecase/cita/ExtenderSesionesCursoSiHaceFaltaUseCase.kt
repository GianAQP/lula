package com.aqpseller.lulaapp.domain.usecase.cita

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

/**
 * Un curso "abierto" (sin `cantidadSesionesTotal`, ej. masajes sin dosis fija) solo genera
 * sesiones hasta un horizonte de 90 días (`GenerarSesionesCursoUseCase`) — esto lo vuelve a
 * completar cuando quedan menos de [DIAS_MINIMOS_POR_DELANTE] días de sesiones generadas por
 * delante, para que el usuario nunca se quede sin programa sin haber tocado nada. No hace nada
 * con un curso de cantidad fija (ese ya sabe exactamente cuándo termina). Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
class ExtenderSesionesCursoSiHaceFaltaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val generarSesionesCursoUseCase: GenerarSesionesCursoUseCase,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    companion object {
        const val DIAS_MINIMOS_POR_DELANTE = 14
    }

    suspend operator fun invoke(actividad: Actividad) {
        val detalle = actividad.detalle as? ActividadDetalle.Cita ?: return
        if (!detalle.esCurso || detalle.cantidadSesionesTotal != null) return
        val existentes = actividadRepository.obtenerSesionesCita(actividad.id)
        val ultimaFecha = existentes.maxOfOrNull { it.fechaOriginal } ?: return
        val hoyEpochDay = DateTimeUtils.hoy().toEpochDays().toLong()
        if (ultimaFecha - hoyEpochDay >= DIAS_MINIMOS_POR_DELANTE) return

        val nuevas = generarSesionesCursoUseCase(actividad.id, detalle, existentes)
        if (nuevas.isEmpty()) return
        actividadRepository.guardarSesionesCita(nuevas)
        nuevas.forEach { sesion ->
            val fechaHoraMillis = DateTimeUtils.combinarFechaYHora(DateTimeUtils.epochDiasAEpochMillis(sesion.fecha), sesion.horario)
            recordatorioScheduler.programarSesionCita(
                actividad.id, actividad.nombre, sesion.numeroSesion, fechaHoraMillis, detalle.recordatorios, detalle.nivelRecordatorio,
            )
        }
    }
}
