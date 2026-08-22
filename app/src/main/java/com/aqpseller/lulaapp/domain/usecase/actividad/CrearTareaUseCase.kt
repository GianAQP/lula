package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import javax.inject.Inject

class CrearTareaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val espacioRepository: EspacioRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        propietario: String,
        nombre: String,
        fechaLimite: Long?,
        importante: Boolean = false,
        urgente: Boolean = false,
        horaRecordatorio: String? = null,
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        recurrencia: RecurrenciaTarea = RecurrenciaTarea.SIN_REPETIR,
        areaDeVidaId: String? = null,
        actividadVinculadaId: String? = null,
    ) {
        val id = IdGenerator.newId()
        val actividad = Actividad(
            id = id,
            tipo = TipoActividad.TAREA,
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
            areaDeVidaId = areaDeVidaId,
            momentoDelDia = null,
            fechaCreacion = DateTimeUtils.ahoraEpochMillis(),
            activa = true,
            detalle = null,
        )
        val detalle = ActividadDetalle.Tarea(
            fechaLimite = fechaLimite,
            importante = importante,
            urgente = urgente,
            horaRecordatorio = horaRecordatorio,
            nivelRecordatorio = nivelRecordatorio,
            recurrencia = recurrencia,
            actividadVinculadaId = actividadVinculadaId,
        )
        actividadRepository.crearTarea(actividad, detalle, propietario)
        if (fechaLimite != null && horaRecordatorio != null) {
            recordatorioScheduler.programarTarea(id, nombre, fechaLimite, horaRecordatorio, nivelRecordatorio)
        }
        // Solo las tareas de un Espacio Familia se sincronizan — lo Personal se queda 100%
        // local (ver `Plan/12-firebase-auth-y-sync.md`).
        if (espacioRepository.obtenerEspacioSiEsMiembro(espacioId, propietario)?.tipo == TipoEspacio.FAMILIA) {
            runCatching { espacioSyncRepository.subirTarea(espacioId, actividad, detalle) }
        }
    }
}
