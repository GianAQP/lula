package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class EliminarActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(actividadId: String, usuarioId: String) {
        // Un Medicamento con varias tomas por día, o una Cita con varios recordatorios, tienen
        // una alarma independiente por horario/anticipación (clave compuesta, ver
        // `RecordatorioScheduler`) — cancelar solo `actividadId` dejaría sonando las demás.
        val actividadActual = actividadRepository.obtenerConDetalle(actividadId)
        val detalle = actividadActual?.detalle
        when (detalle) {
            is ActividadDetalle.Medicamento ->
                // `cancelarMedicamento` cancela también la cadena de "insistir" (recordatorio
                // persistente) — antes acá solo se cancelaba la alarma diaria normal, así que un
                // medicamento con insistencia activa seguía reintentando después de eliminado
                // (el que ya estaba armado en `AlarmManager` no se tocaba). A pedido del usuario.
                detalle.horariosCalculados.forEach { horario -> recordatorioScheduler.cancelarMedicamento(actividadId, horario) }
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
        if (actividadActual != null && espacioRepository.obtenerEspacioSiEsMiembro(actividadActual.espacioId, usuarioId)?.tipo == TipoEspacio.PERSONAL) {
            runCatching { personalSyncRepository.eliminarActividad(actividadId) }
        }
        actividadRepository.eliminar(actividadId, usuarioId)
        recordatorioScheduler.cancelar(actividadId)
    }
}
