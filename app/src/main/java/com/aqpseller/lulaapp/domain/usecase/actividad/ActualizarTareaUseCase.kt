package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.usecase.carecircle.SincronizarSiEstaCompartidaUseCase
import javax.inject.Inject

class ActualizarTareaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val espacioRepository: EspacioRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
    private val personalSyncRepository: PersonalSyncRepository,
    private val sincronizarSiEstaCompartidaUseCase: SincronizarSiEstaCompartidaUseCase,
) {
    suspend operator fun invoke(
        actividadId: String,
        usuarioId: String,
        nombre: String,
        fechaLimite: Long?,
        importante: Boolean,
        urgente: Boolean,
        horaRecordatorio: String? = null,
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        recurrencia: RecurrenciaTarea = RecurrenciaTarea.SIN_REPETIR,
        actividadVinculadaId: String? = null,
    ) {
        val detalle = ActividadDetalle.Tarea(
            fechaLimite = fechaLimite,
            importante = importante,
            urgente = urgente,
            horaRecordatorio = horaRecordatorio,
            nivelRecordatorio = nivelRecordatorio,
            recurrencia = recurrencia,
            actividadVinculadaId = actividadVinculadaId,
        )
        actividadRepository.actualizarTarea(actividadId, nombre, detalle, usuarioId)
        if (fechaLimite != null && horaRecordatorio != null) {
            recordatorioScheduler.programarTarea(actividadId, nombre, fechaLimite, horaRecordatorio, nivelRecordatorio)
        } else {
            recordatorioScheduler.cancelar(actividadId)
        }
        val actividad = actividadRepository.obtenerConDetalle(actividadId) ?: return
        when (espacioRepository.obtenerEspacioSiEsMiembro(actividad.espacioId, usuarioId)?.tipo) {
            TipoEspacio.FAMILIA -> runCatching { espacioSyncRepository.subirTarea(actividad.espacioId, actividad, detalle) }
            TipoEspacio.PERSONAL -> runCatching { personalSyncRepository.subirTarea(actividad, detalle) }
            else -> {}
        }
        runCatching { sincronizarSiEstaCompartidaUseCase(actividadId, usuarioId) }
    }
}
