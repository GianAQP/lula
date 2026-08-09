package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.core.utils.calcularProgresionInicial
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class CrearHabitoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(
        espacioId: String,
        propietario: String,
        nombre: String,
        momentoDelDia: MomentoDelDia,
        frecuencia: FrecuenciaHabito,
        duracionInicialMin: Int?,
        horaRecordatorio: String? = null,
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        duracionObjetivoMin: Int? = null,
        incrementoMin: Int? = null,
        frecuenciaRevisionDias: Int? = null,
        areaDeVidaId: String? = null,
    ) {
        val id = IdGenerator.newId()
        val actividad = Actividad(
            id = id,
            tipo = TipoActividad.HABITO,
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
            momentoDelDia = momentoDelDia,
            fechaCreacion = DateTimeUtils.ahoraEpochMillis(),
            activa = true,
            detalle = null,
        )
        val (duracionActualMin, proximaRevisionEpochDay) = calcularProgresionInicial(
            duracionInicialMin, duracionObjetivoMin, incrementoMin, frecuenciaRevisionDias,
        )
        val detalle = ActividadDetalle.Habito(
            momentoDelDia = momentoDelDia,
            frecuencia = frecuencia,
            duracionInicialMin = duracionInicialMin,
            horaRecordatorio = horaRecordatorio,
            nivelRecordatorio = nivelRecordatorio,
            duracionObjetivoMin = duracionObjetivoMin,
            incrementoMin = incrementoMin,
            frecuenciaRevisionDias = frecuenciaRevisionDias,
            duracionActualMin = duracionActualMin,
            proximaRevisionEpochDay = proximaRevisionEpochDay,
        )
        actividadRepository.crearHabito(actividad, detalle, propietario)
        if (horaRecordatorio != null) {
            recordatorioScheduler.programarHabito(id, nombre, horaRecordatorio, nivelRecordatorio)
        }
    }
}
