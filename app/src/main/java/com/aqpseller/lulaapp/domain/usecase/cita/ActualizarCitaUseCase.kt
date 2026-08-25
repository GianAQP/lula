package com.aqpseller.lulaapp.domain.usecase.cita

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecordatorioCita
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.usecase.carecircle.SincronizarSiEstaCompartidaUseCase
import javax.inject.Inject

class ActualizarCitaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val generarSesionesCursoUseCase: GenerarSesionesCursoUseCase,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
    private val sincronizarSiEstaCompartidaUseCase: SincronizarSiEstaCompartidaUseCase,
) {
    suspend operator fun invoke(
        actividadId: String,
        usuarioId: String,
        nombre: String,
        lugar: String? = null,
        motivo: String? = null,
        fechaHora: Long,
        recordatorios: List<RecordatorioCita> = emptyList(),
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        esCurso: Boolean = false,
        diasSemana: Set<Int> = emptySet(),
        horaSesion: String? = null,
        fechaInicioCurso: Long? = null,
        cantidadSesionesTotal: Int? = null,
    ) {
        val detalle = ActividadDetalle.Cita(
            lugar = lugar,
            motivo = motivo,
            fechaHora = fechaHora,
            recordatorios = recordatorios,
            nivelRecordatorio = nivelRecordatorio,
            esCurso = esCurso,
            diasSemana = diasSemana,
            horaSesion = horaSesion,
            fechaInicioCurso = fechaInicioCurso,
            cantidadSesionesTotal = cantidadSesionesTotal,
        )
        actividadRepository.actualizarCita(actividadId, nombre, detalle, usuarioId)
        val actividad = actividadRepository.obtenerConDetalle(actividadId)
        val esPersonal = actividad != null &&
            espacioRepository.obtenerEspacioSiEsMiembro(actividad.espacioId, usuarioId)?.tipo == TipoEspacio.PERSONAL
        if (esPersonal) {
            runCatching { personalSyncRepository.subirCita(actividad!!, detalle) }
        }
        runCatching { sincronizarSiEstaCompartidaUseCase(actividadId, usuarioId) }
        // Solo hay 3 valores posibles de anticipación — más simple y robusto cancelar los 3
        // (idempotente si no había alarma) que ir a buscar cuáles tenía configurados antes.
        AnticipacionRecordatorio.entries.forEach { recordatorioScheduler.cancelarCita(actividadId, it) }
        if (esCurso) {
            // Editar el patrón de un curso solo afecta a las sesiones que se generan de acá en
            // adelante — las ya generadas (pasadas, marcadas o reprogramadas a mano) no se
            // tocan, ver `Plan/08-decisiones-tecnicas.md`.
            val existentes = actividadRepository.obtenerSesionesCita(actividadId)
            val nuevas = generarSesionesCursoUseCase(actividadId, detalle, existentes)
            if (nuevas.isNotEmpty()) {
                actividadRepository.guardarSesionesCita(nuevas)
                if (esPersonal) {
                    nuevas.forEach { sesion -> runCatching { personalSyncRepository.subirSesionCita(sesion) } }
                }
                runCatching { sincronizarSiEstaCompartidaUseCase(actividadId, usuarioId) }
                nuevas.forEach { sesion ->
                    val fechaHoraSesion = DateTimeUtils.combinarFechaYHora(DateTimeUtils.epochDiasAEpochMillis(sesion.fecha), sesion.horario)
                    recordatorioScheduler.programarSesionCita(actividadId, nombre, sesion.numeroSesion, fechaHoraSesion, recordatorios, nivelRecordatorio)
                }
            }
        } else {
            recordatorioScheduler.programarCita(actividadId, nombre, fechaHora, recordatorios, nivelRecordatorio)
        }
    }
}
