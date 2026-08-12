package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.horariosParaFecha
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
                activos.mapNotNull { actividad ->
                    val detalle = actividad.detalle as? ActividadDetalle.Medicamento ?: return@mapNotNull null
                    val horariosDelDia = horariosParaFecha(detalle, DateTimeUtils.hoy())
                    if (horariosDelDia.isEmpty()) return@mapNotNull null
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
