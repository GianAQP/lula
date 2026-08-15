package com.aqpseller.lulaapp.domain.usecase.notificaciones

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.horariosParaFecha
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Vuelve a programar TODOS los recordatorios activos (Hábito, Tarea, Medicamento, Cita, Fecha
 * importante, Meta, cierre de día, franjas del día) desde cero. Antes esto solo vivía dentro de
 * `BootReceiver` (un reinicio del teléfono borra todas las alarmas de `AlarmManager`) — pero el
 * usuario reportó que los recordatorios sonaban bien al probarlos y dejaban de sonar "al día
 * siguiente" sin que el teléfono se hubiera reiniciado. Causa real: algunos fabricantes
 * (Motorola incluido, confirmado con el dispositivo de prueba) "fuerzan detener" la app en
 * segundo plano para ahorrar batería — eso TAMBIÉN cancela todas las alarmas pendientes, igual
 * que un reinicio, pero sin disparar `BOOT_COMPLETED`. Sin este use case, nada volvía a armar
 * los recordatorios hasta el próximo reinicio real del teléfono. Ahora se llama también cada vez
 * que se abre la app (`AppViewModel`), así que basta con volver a abrir Lula para repararlos,
 * sin esperar a un reinicio. Ver `Plan/08-decisiones-tecnicas.md`.
 */
class ReprogramarTodosLosRecordatoriosUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val metaRepository: MetaRepository,
    private val ajustesRepository: AjustesRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
) {
    suspend operator fun invoke() {
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
            // Antes se reprogramaban TODOS los `horariosCalculados` sin revisar `fechaFin` — un
            // medicamento cuyo tratamiento ya terminó se volvía a armar entero (todas sus horas)
            // cada vez que se abría la app, porque "activa" no se apaga solo cuando pasa la
            // fecha fin. `horariosParaFecha` es el mismo filtro que ya usa "Medicamentos de hoy"
            // para saber qué horarios de verdad siguen vigentes. Ver `08-decisiones-tecnicas.md`.
            val horariosDeHoy = horariosParaFecha(detalle, DateTimeUtils.hoy())
            horariosDeHoy.forEachIndexed { index, horario ->
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

        // No tenía ningún camino que la reprogramara — se sumó junto con el resto acá.
        metaRepository.observarPorEspacio(sesion.espacioId).first().forEach { meta ->
            if (meta.fechaLimite != null && meta.valorActual < meta.valorObjetivo) {
                recordatorioScheduler.programarMeta(meta.id, meta.nombre, meta.fechaLimite, meta.nivelRecordatorio)
            }
        }
    }
}
