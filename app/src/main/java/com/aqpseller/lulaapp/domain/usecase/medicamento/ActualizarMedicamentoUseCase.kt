package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.calcularFechaFinPorCantidadDosis
import com.aqpseller.lulaapp.core.utils.calcularHorariosPorComida
import com.aqpseller.lulaapp.core.utils.calcularHorariosPorIntervalo
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import javax.inject.Inject

class ActualizarMedicamentoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
) {
    suspend operator fun invoke(
        actividadId: String,
        nombreMedicamento: String,
        dosis: String,
        modoFrecuencia: ModoFrecuenciaMedicamento,
        intervaloHoras: Int? = null,
        horaPrimeraDosis: String? = null,
        comidasRelacionadas: List<ComidaRelacionada> = emptyList(),
        horaDesayuno: String? = null,
        horaAlmuerzo: String? = null,
        horaCena: String? = null,
        fechaInicio: Long,
        fechaFin: Long? = null,
        cantidadDosisTotal: Int? = null,
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        recordatorioPersistente: Boolean = false,
        intervaloPersistenciaMin: Int? = null,
        usuarioId: String,
    ) {
        val detalleAnterior = obtenerDetalleActividadUseCase(actividadId)?.detalle as? ActividadDetalle.Medicamento
        detalleAnterior?.horariosCalculados?.forEach { horario ->
            recordatorioScheduler.cancelarMedicamento(actividadId, horario)
        }

        val horarios = when (modoFrecuencia) {
            ModoFrecuenciaMedicamento.INTERVALO_HORAS ->
                if (horaPrimeraDosis != null && intervaloHoras != null) {
                    calcularHorariosPorIntervalo(horaPrimeraDosis, intervaloHoras)
                } else {
                    emptyList()
                }
            ModoFrecuenciaMedicamento.RELACION_COMIDA ->
                calcularHorariosPorComida(comidasRelacionadas, horaDesayuno, horaAlmuerzo, horaCena)
        }
        val fechaFinFinal = if (cantidadDosisTotal != null) {
            calcularFechaFinPorCantidadDosis(fechaInicio, horarios, cantidadDosisTotal)
        } else {
            fechaFin
        }

        val detalle = ActividadDetalle.Medicamento(
            nombreMedicamento = nombreMedicamento,
            dosis = dosis,
            modoFrecuencia = modoFrecuencia,
            intervaloHoras = intervaloHoras,
            horaPrimeraDosis = horaPrimeraDosis,
            horariosCalculados = horarios,
            comidasRelacionadas = comidasRelacionadas,
            fechaInicio = fechaInicio,
            fechaFin = fechaFinFinal,
            cantidadDosisTotal = cantidadDosisTotal,
            nivelRecordatorio = nivelRecordatorio,
            recordatorioPersistente = recordatorioPersistente,
            intervaloPersistenciaMin = intervaloPersistenciaMin,
        )
        actividadRepository.actualizarMedicamento(actividadId, nombreMedicamento, detalle, usuarioId)

        horarios.forEachIndexed { index, horario ->
            recordatorioScheduler.programarMedicamento(
                actividadId, nombreMedicamento, horario, instruccionParaHorario(detalle, index), nivelRecordatorio,
                recordatorioPersistente, intervaloPersistenciaMin,
            )
        }
    }
}
