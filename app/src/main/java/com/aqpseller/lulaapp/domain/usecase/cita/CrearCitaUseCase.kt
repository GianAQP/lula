package com.aqpseller.lulaapp.domain.usecase.cita

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.RecordatorioCita
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class CrearCitaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val generarSesionesCursoUseCase: GenerarSesionesCursoUseCase,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        propietario: String,
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
        val id = IdGenerator.newId()
        val actividad = Actividad(
            id = id,
            tipo = TipoActividad.CITA,
            espacioId = espacioId,
            nombre = nombre,
            propietario = propietario,
            responsables = listOf(propietario),
            puedeVer = emptyList(),
            puedeRecordar = emptyList(),
            estado = EstadoActividad.SIN_CONFIRMAR,
            privacidad = Privacidad.SOLO_YO,
            syncStatus = SyncStatus.LOCAL,
            esPremiumFeature = false,
            areaDeVidaId = null,
            momentoDelDia = null,
            fechaCreacion = DateTimeUtils.ahoraEpochMillis(),
            activa = true,
            detalle = null,
        )
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
        actividadRepository.crearCita(actividad, detalle, propietario)
        val esPersonal = espacioRepository.obtenerEspacioSiEsMiembro(espacioId, propietario)?.tipo == TipoEspacio.PERSONAL
        if (esPersonal) {
            runCatching { personalSyncRepository.subirCita(actividad, detalle) }
        }
        if (esCurso) {
            val sesiones = generarSesionesCursoUseCase(id, detalle, emptyList())
            actividadRepository.guardarSesionesCita(sesiones)
            if (esPersonal) {
                sesiones.forEach { sesion -> runCatching { personalSyncRepository.subirSesionCita(sesion) } }
            }
            sesiones.forEach { sesion ->
                val fechaHoraSesion = DateTimeUtils.combinarFechaYHora(DateTimeUtils.epochDiasAEpochMillis(sesion.fecha), sesion.horario)
                recordatorioScheduler.programarSesionCita(id, nombre, sesion.numeroSesion, fechaHoraSesion, recordatorios, nivelRecordatorio)
            }
        } else {
            recordatorioScheduler.programarCita(id, nombre, fechaHora, recordatorios, nivelRecordatorio)
        }
    }
}
