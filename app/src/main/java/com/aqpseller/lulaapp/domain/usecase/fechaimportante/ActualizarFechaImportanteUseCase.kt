package com.aqpseller.lulaapp.domain.usecase.fechaimportante

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.TipoAviso
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class ActualizarFechaImportanteUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(
        actividadId: String,
        usuarioId: String,
        nombre: String,
        fechaBase: Long,
        recurrencia: Recurrencia,
        horaNotificacion: String,
        anticipacion: AnticipacionRecordatorio,
        tipoAviso: TipoAviso,
    ) {
        val detalle = ActividadDetalle.FechaImportante(
            recurrencia = recurrencia,
            fechaBase = fechaBase,
            horaNotificacion = horaNotificacion,
            anticipacion = anticipacion,
            tipoAviso = tipoAviso,
        )
        actividadRepository.actualizarFechaImportante(actividadId, nombre, detalle, usuarioId)
        recordatorioScheduler.cancelar(actividadId)
        recordatorioScheduler.programarFechaImportante(actividadId, nombre, fechaBase, horaNotificacion, anticipacion, recurrencia, tipoAviso)
    }
}
