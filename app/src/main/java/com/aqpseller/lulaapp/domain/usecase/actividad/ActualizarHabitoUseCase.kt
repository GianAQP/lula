package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.calcularProgresionInicial
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class ActualizarHabitoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(
        actividadId: String,
        usuarioId: String,
        nombre: String,
        momentoDelDia: MomentoDelDia,
        duracionInicialMin: Int?,
        horaRecordatorio: String? = null,
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        duracionObjetivoMin: Int? = null,
        incrementoMin: Int? = null,
        frecuenciaRevisionDias: Int? = null,
    ) {
        // Si la configuración de progresión no cambió, se preserva el progreso ya hecho
        // (duración actual y próxima revisión) — no se reinicia solo por editar el nombre o el
        // recordatorio. Si cambió (o se activa por primera vez), se recalcula desde cero.
        val detalleActual = obtenerDetalleActividadUseCase(actividadId)?.detalle as? ActividadDetalle.Habito
        val progresionSinCambios = detalleActual?.esProgresivo == true &&
            detalleActual.duracionObjetivoMin == duracionObjetivoMin &&
            detalleActual.incrementoMin == incrementoMin &&
            detalleActual.frecuenciaRevisionDias == frecuenciaRevisionDias
        val (duracionActualMin, proximaRevisionEpochDay) = if (progresionSinCambios) {
            detalleActual!!.duracionActualMin to detalleActual.proximaRevisionEpochDay
        } else {
            calcularProgresionInicial(duracionInicialMin, duracionObjetivoMin, incrementoMin, frecuenciaRevisionDias)
        }

        val detalle = ActividadDetalle.Habito(
            momentoDelDia = momentoDelDia,
            frecuencia = FrecuenciaHabito.DIARIA,
            duracionInicialMin = duracionInicialMin,
            horaRecordatorio = horaRecordatorio,
            nivelRecordatorio = nivelRecordatorio,
            duracionObjetivoMin = duracionObjetivoMin,
            incrementoMin = incrementoMin,
            frecuenciaRevisionDias = frecuenciaRevisionDias,
            duracionActualMin = duracionActualMin,
            proximaRevisionEpochDay = proximaRevisionEpochDay,
        )
        actividadRepository.actualizarHabito(actividadId, nombre, detalle, usuarioId)
        if (horaRecordatorio != null) {
            recordatorioScheduler.programarHabito(actividadId, nombre, horaRecordatorio, nivelRecordatorio)
        } else {
            recordatorioScheduler.cancelar(actividadId)
        }
        val actividad = actividadRepository.obtenerConDetalle(actividadId) ?: return
        if (espacioRepository.obtenerEspacioSiEsMiembro(actividad.espacioId, usuarioId)?.tipo == TipoEspacio.PERSONAL) {
            runCatching { personalSyncRepository.subirHabito(actividad, detalle) }
        }
    }
}
