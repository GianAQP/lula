package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.indiceUltimaDosisEnDiaFinal
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MedicamentoDeHoy
import com.aqpseller.lulaapp.domain.model.TomaDeHoy
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Medicamentos activos vigentes hoy (entre `fechaInicio` y `fechaFin`), con cada dosis del
 * día ya resuelta contra `toma_medicamento` — usado tanto por Hoy como por "Mi salud" para no
 * duplicar esta lógica en dos ViewModels.
 */
class ObtenerMedicamentosDeHoyUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(espacioId: String): Flow<List<MedicamentoDeHoy>> =
        actividadRepository.observarMedicamentos(espacioId).flatMapLatest { medicamentos ->
            val activos = medicamentos.filter { it.activa }
            val ids = activos.map { it.id }
            actividadRepository.observarTomasDeHoy(ids).map { tomas ->
                val tomasPorClave = tomas.associateBy { it.actividadId to it.horario }
                val hoy = DateTimeUtils.hoy().toEpochDays().toLong()
                activos.mapNotNull { actividad ->
                    val detalle = actividad.detalle as? ActividadDetalle.Medicamento ?: return@mapNotNull null
                    val fechaInicioDay = DateTimeUtils.epochMillisToLocalDate(detalle.fechaInicio).toEpochDays().toLong()
                    val fechaFinDay = detalle.fechaFin?.let { DateTimeUtils.epochMillisToLocalDate(it).toEpochDays().toLong() }
                    if (hoy < fechaInicioDay || (fechaFinDay != null && hoy > fechaFinDay)) return@mapNotNull null
                    // Si el fin se calculó por "cantidad de dosis", el último día no necesariamente
                    // agota todos los horarios del día (ej. 4 dosis a 3 por día: el 2do día solo
                    // cuenta la primera) — sin este recorte se mostraban de más, ver `08-decisiones-tecnicas.md`.
                    val horariosDelDia = if (
                        detalle.cantidadDosisTotal != null && fechaFinDay != null && hoy == fechaFinDay &&
                        detalle.horariosCalculados.isNotEmpty()
                    ) {
                        val indiceCorte = indiceUltimaDosisEnDiaFinal(detalle.horariosCalculados, detalle.cantidadDosisTotal)
                        detalle.horariosCalculados.take(indiceCorte + 1)
                    } else {
                        detalle.horariosCalculados
                    }
                    MedicamentoDeHoy(
                        actividadId = actividad.id,
                        nombre = actividad.nombre,
                        dosis = detalle.dosis,
                        nivelRecordatorio = detalle.nivelRecordatorio,
                        tomas = horariosDelDia.mapIndexed { index, horario ->
                            TomaDeHoy(
                                horario = horario,
                                instruccion = instruccionParaHorario(detalle, index),
                                estado = tomasPorClave[actividad.id to horario]?.estado ?: EstadoActividad.SIN_CONFIRMAR,
                            )
                        },
                    )
                }
            }
        }
}
