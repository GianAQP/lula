package com.aqpseller.lulaapp.domain.usecase.fechaimportante

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.TipoAviso
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class ActualizarFechaImportanteUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
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
        val actividad = actividadRepository.obtenerConDetalle(actividadId)
        if (actividad != null && espacioRepository.obtenerEspacioSiEsMiembro(actividad.espacioId, usuarioId)?.tipo == TipoEspacio.PERSONAL) {
            runCatching { personalSyncRepository.subirFechaImportante(actividad, detalle) }
        }
        recordatorioScheduler.cancelar(actividadId)
        recordatorioScheduler.programarFechaImportante(actividadId, nombre, fechaBase, horaNotificacion, anticipacion, recurrencia, tipoAviso)
    }
}
