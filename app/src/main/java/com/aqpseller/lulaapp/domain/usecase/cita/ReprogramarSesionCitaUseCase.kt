package com.aqpseller.lulaapp.domain.usecase.cita

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import javax.inject.Inject

/** Mueve UNA sesión de un curso de Cita a otro día — no toca `fechaOriginal` ni el resto del
 * curso (decisión explícita del usuario, ver `Plan/08-decisiones-tecnicas.md`): reprogramar
 * "sesión 7" solo cambia cuándo es la 7, el conteo total y las demás sesiones siguen igual. */
class ReprogramarSesionCitaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(actividadId: String, numeroSesion: Int, nuevaFechaEpochDay: Long, usuarioId: String) {
        val actividad = obtenerDetalleActividadUseCase(actividadId) ?: return
        val detalle = actividad.detalle as? ActividadDetalle.Cita ?: return
        AnticipacionRecordatorio.entries.forEach { recordatorioScheduler.cancelarSesionCita(actividadId, numeroSesion, it) }
        actividadRepository.reprogramarSesionCita(actividadId, numeroSesion, nuevaFechaEpochDay, usuarioId)
        val horario = detalle.horaSesion ?: "09:00"
        val fechaHoraMillis = DateTimeUtils.combinarFechaYHora(DateTimeUtils.epochDiasAEpochMillis(nuevaFechaEpochDay), horario)
        recordatorioScheduler.programarSesionCita(actividadId, actividad.nombre, numeroSesion, fechaHoraMillis, detalle.recordatorios, detalle.nivelRecordatorio)
    }
}
