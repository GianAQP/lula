package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class EliminarActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(actividadId: String, usuarioId: String) {
        // Un Medicamento con varias tomas por día, o una Cita con varios recordatorios, tienen
        // una alarma independiente por horario/anticipación (clave compuesta, ver
        // `RecordatorioScheduler`) — cancelar solo `actividadId` dejaría sonando las demás.
        val detalle = actividadRepository.obtenerConDetalle(actividadId)?.detalle
        when (detalle) {
            is ActividadDetalle.Medicamento ->
                detalle.horariosCalculados.forEach { horario -> recordatorioScheduler.cancelar(actividadId, horario) }
            is ActividadDetalle.Cita ->
                if (detalle.esCurso) {
                    actividadRepository.obtenerSesionesCita(actividadId).forEach { sesion ->
                        AnticipacionRecordatorio.entries.forEach { recordatorioScheduler.cancelarSesionCita(actividadId, sesion.numeroSesion, it) }
                    }
                } else {
                    AnticipacionRecordatorio.entries.forEach { recordatorioScheduler.cancelarCita(actividadId, it) }
                }
            else -> Unit
        }
        actividadRepository.eliminar(actividadId, usuarioId)
        recordatorioScheduler.cancelar(actividadId)
    }
}
