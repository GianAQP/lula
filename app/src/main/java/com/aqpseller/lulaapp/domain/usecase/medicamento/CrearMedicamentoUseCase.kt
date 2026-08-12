package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.core.utils.calcularFechaFinPorCantidadDosis
import com.aqpseller.lulaapp.core.utils.calcularHorariosPorComida
import com.aqpseller.lulaapp.core.utils.calcularHorariosPorIntervalo
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

/**
 * Regla de producto (`Plan/04-roadmap-fases.md`): Lula solo recuerda y registra lo que el
 * usuario indica — nunca sugiere cambiar dosis, horarios ni suspender un medicamento.
 */
class CrearMedicamentoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(
        espacioId: String,
        propietario: String,
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
    ) {
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
        // "Cantidad de dosis" y "fecha de fin" son mutuamente excluyentes en la pantalla — si
        // hay cantidad, la fecha de fin se calcula sola en vez de usar la que llegó por parámetro.
        val fechaFinFinal = if (cantidadDosisTotal != null) {
            calcularFechaFinPorCantidadDosis(fechaInicio, horarios, cantidadDosisTotal, modoFrecuencia, horaPrimeraDosis)
        } else {
            fechaFin
        }

        val id = IdGenerator.newId()
        val actividad = Actividad(
            id = id,
            tipo = TipoActividad.MEDICAMENTO,
            espacioId = espacioId,
            nombre = nombreMedicamento,
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
        actividadRepository.crearMedicamento(actividad, detalle, propietario)

        horarios.forEachIndexed { index, horario ->
            recordatorioScheduler.programarMedicamento(
                id, nombreMedicamento, horario, instruccionParaHorario(detalle, index), nivelRecordatorio,
                recordatorioPersistente, intervaloPersistenciaMin,
            )
        }
    }
}
