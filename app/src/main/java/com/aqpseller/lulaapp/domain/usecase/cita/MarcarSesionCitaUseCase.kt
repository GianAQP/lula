package com.aqpseller.lulaapp.domain.usecase.cita

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import javax.inject.Inject

/** Marca cumplida/no cumplida UNA sesión de un curso de Cita — a diferencia de `MarcarActividadUseCase`,
 * necesita `numeroSesion` porque el curso tiene varias sesiones, cada una con su propio estado.
 * También sirve para deshacer una marca puesta por error (volver a `SIN_CONFIRMAR`) — en ese
 * caso, si la sesión todavía no pasó, se vuelve a programar su recordatorio (se había cancelado
 * al marcarla). */
class MarcarSesionCitaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(actividadId: String, numeroSesion: Int, estado: EstadoActividad, usuarioId: String) {
        actividadRepository.marcarSesionCita(actividadId, numeroSesion, estado, usuarioId)
        val actividadParaSync = actividadRepository.obtenerConDetalle(actividadId)
        if (actividadParaSync != null && espacioRepository.obtenerEspacioSiEsMiembro(actividadParaSync.espacioId, usuarioId)?.tipo == TipoEspacio.PERSONAL) {
            actividadRepository.obtenerSesionesCita(actividadId).firstOrNull { it.numeroSesion == numeroSesion }?.let { sesion ->
                runCatching { personalSyncRepository.subirSesionCita(sesion) }
            }
        }
        if (estado != EstadoActividad.SIN_CONFIRMAR) {
            AnticipacionRecordatorio.entries.forEach {
                recordatorioScheduler.cancelarSesionCita(actividadId, numeroSesion, it)
            }
        } else {
            val actividad = obtenerDetalleActividadUseCase(actividadId) ?: return
            val detalle = actividad.detalle as? ActividadDetalle.Cita ?: return
            val sesion = actividadRepository.obtenerSesionesCita(actividadId).firstOrNull { it.numeroSesion == numeroSesion } ?: return
            val fechaHoraSesion = DateTimeUtils.combinarFechaYHora(DateTimeUtils.epochDiasAEpochMillis(sesion.fecha), sesion.horario)
            if (fechaHoraSesion > DateTimeUtils.ahoraEpochMillis()) {
                recordatorioScheduler.programarSesionCita(actividadId, actividad.nombre, numeroSesion, fechaHoraSesion, detalle.recordatorios, detalle.nivelRecordatorio)
            }
        }
    }
}
