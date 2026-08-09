package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class PausarReanudarActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(actividadId: String, activa: Boolean, usuarioId: String) {
        actividadRepository.pausarReanudar(actividadId, activa, usuarioId)

        if (!activa) {
            recordatorioScheduler.cancelar(actividadId)
            return
        }

        val actividad = actividadRepository.obtenerConDetalle(actividadId) ?: return
        when (val detalle = actividad.detalle) {
            is ActividadDetalle.Habito -> detalle.horaRecordatorio?.let { hora ->
                recordatorioScheduler.programarHabito(actividadId, actividad.nombre, hora, detalle.nivelRecordatorio)
            }
            is ActividadDetalle.Tarea -> {
                val fechaLimite = detalle.fechaLimite
                val hora = detalle.horaRecordatorio
                if (fechaLimite != null && hora != null) {
                    recordatorioScheduler.programarTarea(actividadId, actividad.nombre, fechaLimite, hora, detalle.nivelRecordatorio)
                }
            }
            else -> Unit
        }
    }
}
