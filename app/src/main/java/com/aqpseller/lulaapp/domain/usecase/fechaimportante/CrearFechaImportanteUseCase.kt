package com.aqpseller.lulaapp.domain.usecase.fechaimportante

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoAviso
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class CrearFechaImportanteUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(
        espacioId: String,
        propietario: String,
        nombre: String,
        fechaBase: Long,
        recurrencia: Recurrencia = Recurrencia.ANUAL,
        horaNotificacion: String = "09:00",
        anticipacion: AnticipacionRecordatorio = AnticipacionRecordatorio.MISMO_DIA,
        tipoAviso: TipoAviso = TipoAviso.MENSAJE_SILENCIOSO,
    ) {
        val id = IdGenerator.newId()
        val actividad = Actividad(
            id = id,
            tipo = TipoActividad.FECHA_IMPORTANTE,
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
        val detalle = ActividadDetalle.FechaImportante(
            recurrencia = recurrencia,
            fechaBase = fechaBase,
            horaNotificacion = horaNotificacion,
            anticipacion = anticipacion,
            tipoAviso = tipoAviso,
        )
        actividadRepository.crearFechaImportante(actividad, detalle, propietario)
        recordatorioScheduler.programarFechaImportante(id, nombre, fechaBase, horaNotificacion, anticipacion, recurrencia, tipoAviso)
    }
}
