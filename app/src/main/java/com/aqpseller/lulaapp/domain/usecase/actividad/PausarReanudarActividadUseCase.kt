package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class PausarReanudarActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(actividadId: String, activa: Boolean, usuarioId: String) {
        // Antes de pausar, se necesita el detalle VIEJO (con sus horarios/sesiones) para cancelar
        // — después de `pausarReanudar` ya no importa, pero las alarmas siguen siendo las mismas.
        val actividadAntes = actividadRepository.obtenerConDetalle(actividadId)

        actividadRepository.pausarReanudar(actividadId, activa, usuarioId)

        val actividadActualizada = actividadRepository.obtenerConDetalle(actividadId)
        if (actividadActualizada != null &&
            espacioRepository.obtenerEspacioSiEsMiembro(actividadActualizada.espacioId, usuarioId)?.tipo == TipoEspacio.PERSONAL
        ) {
            runCatching { personalSyncRepository.subirActividadSegunTipo(actividadActualizada) }
        }

        if (!activa) {
            // Un Medicamento con varias tomas por día, o una Cita con varios recordatorios,
            // tienen una alarma independiente por horario/anticipación (clave compuesta, ver
            // `RecordatorioScheduler`) — cancelar solo `actividadId` (como antes) dejaba sonando
            // las demás: "Pausar" parecía funcionar (la pantalla lo mostraba Pausado) pero las
            // alarmas seguían armadas en `AlarmManager`, sin ninguna pista visible del error.
            // Mismo patrón ya usado en `EliminarActividadUseCase`. Ver `Plan/08-decisiones-tecnicas.md`.
            when (val detalle = actividadAntes?.detalle) {
                is ActividadDetalle.Medicamento ->
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
            recordatorioScheduler.cancelar(actividadId)
            return
        }

        val actividad = actividadActualizada ?: return
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
            is ActividadDetalle.Medicamento ->
                detalle.horariosCalculados.forEachIndexed { index, horario ->
                    recordatorioScheduler.programarMedicamento(
                        actividadId, actividad.nombre, horario, instruccionParaHorario(detalle, index), detalle.nivelRecordatorio,
                    )
                }
            else -> Unit
        }
    }
}
