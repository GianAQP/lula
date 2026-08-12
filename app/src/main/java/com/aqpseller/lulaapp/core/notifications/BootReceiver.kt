package com.aqpseller.lulaapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `AlarmManager` borra todas las alarmas pendientes al reiniciar el dispositivo — sin esto,
 * los recordatorios dejarían de sonar en silencio tras cada reinicio, un bug invisible que
 * nadie notaría hasta extrañar el recordatorio. Vuelve a programar todo lo activo con hora.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var actividadRepository: ActividadRepository

    @Inject lateinit var obtenerSesionActualUseCase: ObtenerSesionActualUseCase

    @Inject lateinit var recordatorioScheduler: RecordatorioScheduler

    @Inject lateinit var ajustesRepository: AjustesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { reprogramarTodo() }
            pendingResult.finish()
        }
    }

    private suspend fun reprogramarTodo() {
        val sesion = obtenerSesionActualUseCase()

        ajustesRepository.observarHoraRecordatorioCierreDia().first()?.let { hora ->
            recordatorioScheduler.programarRecordatorioCierreDia(hora)
        }
        MomentoDelDia.entries.forEach { momento ->
            ajustesRepository.observarHoraRecordatorioFranja(momento).first()?.let { hora ->
                recordatorioScheduler.programarRecordatorioFranja(momento, hora)
            }
        }

        actividadRepository.observarHabitos(sesion.espacioId).first().forEach { habito ->
            val detalle = habito.detalle as? ActividadDetalle.Habito
            val hora = detalle?.horaRecordatorio
            if (habito.activa && hora != null) {
                recordatorioScheduler.programarHabito(habito.id, habito.nombre, hora, detalle.nivelRecordatorio)
            }
        }

        actividadRepository.observarTareas(sesion.espacioId).first().forEach { tarea ->
            val detalle = tarea.detalle as? ActividadDetalle.Tarea
            val hora = detalle?.horaRecordatorio
            val fechaLimite = detalle?.fechaLimite
            if (tarea.activa && hora != null && fechaLimite != null && tarea.estado != EstadoActividad.CONFIRMADO) {
                recordatorioScheduler.programarTarea(tarea.id, tarea.nombre, fechaLimite, hora, detalle.nivelRecordatorio)
            }
        }

        actividadRepository.observarMedicamentos(sesion.espacioId).first().forEach { medicamento ->
            val detalle = medicamento.detalle as? ActividadDetalle.Medicamento ?: return@forEach
            if (!medicamento.activa) return@forEach
            detalle.horariosCalculados.forEachIndexed { index, horario ->
                recordatorioScheduler.programarMedicamento(
                    medicamento.id,
                    medicamento.nombre,
                    horario,
                    instruccionParaHorario(detalle, index),
                    detalle.nivelRecordatorio,
                )
            }
        }

        actividadRepository.observarCitas(sesion.espacioId).first().forEach { cita ->
            val detalle = cita.detalle as? ActividadDetalle.Cita ?: return@forEach
            if (cita.activa && cita.estado != EstadoActividad.CONFIRMADO && detalle.fechaHora > DateTimeUtils.ahoraEpochMillis()) {
                recordatorioScheduler.programarCita(
                    cita.id,
                    cita.nombre,
                    detalle.fechaHora,
                    detalle.recordatorios,
                    detalle.nivelRecordatorio,
                )
            }
        }

        actividadRepository.observarFechasImportantes(sesion.espacioId).first().forEach { fechaImportante ->
            val detalle = fechaImportante.detalle as? ActividadDetalle.FechaImportante ?: return@forEach
            if (fechaImportante.activa) {
                recordatorioScheduler.programarFechaImportante(
                    fechaImportante.id,
                    fechaImportante.nombre,
                    detalle.fechaBase,
                    detalle.horaNotificacion,
                    detalle.anticipacion,
                    detalle.recurrencia,
                    detalle.tipoAviso,
                )
            }
        }
    }
}
