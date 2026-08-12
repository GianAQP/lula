package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.siguienteFechaTareaRecurrente
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class MarcarActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(actividadId: String, estado: EstadoActividad, usuarioId: String, fechaCompletado: Long? = null) {
        actividadRepository.marcarEstado(actividadId, estado, usuarioId, fechaCompletado)
        if (estado != EstadoActividad.CONFIRMADO) return
        avanzarSiEsTareaRecurrente(actividadId, usuarioId)
    }

    /**
     * Si es una Tarea recurrente (pagar la luz, agua, etc.), al marcarla la deja `SIN_CONFIRMAR`
     * de nuevo con la próxima fecha — igual que un Hábito, pero sin depender de una fecha diaria
     * fija. Ver `08-decisiones-tecnicas.md`.
     */
    private suspend fun avanzarSiEsTareaRecurrente(actividadId: String, usuarioId: String) {
        val actividad = actividadRepository.obtenerConDetalle(actividadId) ?: return
        val detalle = actividad.detalle as? ActividadDetalle.Tarea ?: return
        if (detalle.recurrencia == RecurrenciaTarea.SIN_REPETIR) return
        val fechaBase = detalle.fechaLimite ?: return
        val siguienteFecha = siguienteFechaTareaRecurrente(fechaBase, detalle.recurrencia)

        actividadRepository.reprogramarTareaRecurrente(actividadId, siguienteFecha, usuarioId)

        val hora = detalle.horaRecordatorio
        if (hora != null) {
            recordatorioScheduler.programarTarea(actividadId, actividad.nombre, siguienteFecha, hora, detalle.nivelRecordatorio)
        }
    }
}
