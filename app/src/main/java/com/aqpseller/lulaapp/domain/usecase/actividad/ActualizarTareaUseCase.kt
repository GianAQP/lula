package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class ActualizarTareaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(
        actividadId: String,
        usuarioId: String,
        nombre: String,
        fechaLimite: Long?,
        importante: Boolean,
        urgente: Boolean,
        horaRecordatorio: String? = null,
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        recurrencia: RecurrenciaTarea = RecurrenciaTarea.SIN_REPETIR,
        actividadVinculadaId: String? = null,
    ) {
        val detalle = ActividadDetalle.Tarea(
            fechaLimite = fechaLimite,
            importante = importante,
            urgente = urgente,
            horaRecordatorio = horaRecordatorio,
            nivelRecordatorio = nivelRecordatorio,
            recurrencia = recurrencia,
            actividadVinculadaId = actividadVinculadaId,
        )
        actividadRepository.actualizarTarea(actividadId, nombre, detalle, usuarioId)
        if (fechaLimite != null && horaRecordatorio != null) {
            recordatorioScheduler.programarTarea(actividadId, nombre, fechaLimite, horaRecordatorio, nivelRecordatorio)
        } else {
            recordatorioScheduler.cancelar(actividadId)
        }
    }
}
