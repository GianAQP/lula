package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class MarcarTomaMedicamentoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(actividadId: String, horario: String, estado: EstadoActividad, usuarioId: String) {
        actividadRepository.marcarToma(actividadId, horario, estado, usuarioId)
        // Si quedó resuelta (tomada u omitida), el recordatorio persistente ya no tiene nada
        // que insistir — se cancela ahora en vez de esperar a que la próxima insistencia
        // programada se dé cuenta sola (ver `RecordatorioReceiver`).
        if (estado != EstadoActividad.SIN_CONFIRMAR) {
            recordatorioScheduler.cancelarRenotificacionMedicamento(actividadId, horario)
        }
    }
}
