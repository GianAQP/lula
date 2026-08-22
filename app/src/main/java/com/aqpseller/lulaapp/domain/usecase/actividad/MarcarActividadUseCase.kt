package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.siguienteFechaTareaRecurrente
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class MarcarActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val espacioRepository: EspacioRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(actividadId: String, estado: EstadoActividad, usuarioId: String, fechaCompletado: Long? = null) {
        actividadRepository.marcarEstado(actividadId, estado, usuarioId, fechaCompletado)
        sincronizarSiCorresponde(actividadId, estado, usuarioId)
        if (estado != EstadoActividad.CONFIRMADO) return
        avanzarSiEsTareaRecurrente(actividadId, usuarioId)
    }

    /** Tareas de un Espacio Familia y Hábitos/Tareas del Espacio Personal se respaldan — ver
     * `Plan/12-firebase-auth-y-sync.md`. */
    private suspend fun sincronizarSiCorresponde(actividadId: String, estado: EstadoActividad, usuarioId: String) {
        val actividad = actividadRepository.obtenerConDetalle(actividadId) ?: return
        val tipoEspacio = espacioRepository.obtenerEspacioSiEsMiembro(actividad.espacioId, usuarioId)?.tipo ?: return

        when (actividad.tipo) {
            TipoActividad.TAREA -> {
                val detalle = actividad.detalle as? ActividadDetalle.Tarea ?: return
                when (tipoEspacio) {
                    TipoEspacio.FAMILIA -> runCatching { espacioSyncRepository.subirTarea(actividad.espacioId, actividad, detalle) }
                    TipoEspacio.PERSONAL -> runCatching { personalSyncRepository.subirTarea(actividad, detalle) }
                    else -> {}
                }
            }
            TipoActividad.HABITO -> {
                if (tipoEspacio != TipoEspacio.PERSONAL) return
                val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
                runCatching { personalSyncRepository.subirRegistroHabito(actividadId, fechaHoy, estado) }
            }
            else -> {}
        }
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
