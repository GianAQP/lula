package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Cierra solas las Tareas vinculadas a un Medicamento/Cita (caso "cuidar a alguien por un
 * tiempo", ver `08-decisiones-tecnicas.md`) cuando ese Medicamento/Cita ya terminó su ciclo de
 * vida — Cita: ya pasó su fecha y hora; Medicamento: ya pasó su fecha de fin. Se corre cada vez
 * que se abre Hoy (ver `HomeViewModel`), no hay tarea en segundo plano — es barato porque solo
 * mira tareas que tienen vínculo y todavía no están completadas.
 */
class CerrarTareasVinculadasVencidasUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val marcarActividadUseCase: MarcarActividadUseCase,
) {
    suspend operator fun invoke(espacioId: String, usuarioId: String) {
        val tareasConVinculo = actividadRepository.observarTareas(espacioId).first()
            .filter { it.estado != EstadoActividad.CONFIRMADO }
            .mapNotNull { tarea ->
                (tarea.detalle as? ActividadDetalle.Tarea)?.actividadVinculadaId?.let { tarea.id to it }
            }

        for ((tareaId, actividadVinculadaId) in tareasConVinculo) {
            val vinculada = actividadRepository.obtenerConDetalle(actividadVinculadaId) ?: continue
            if (yaTerminoSuCicloDeVida(vinculada)) {
                marcarActividadUseCase(tareaId, EstadoActividad.CONFIRMADO, usuarioId)
            }
        }
    }

    private fun yaTerminoSuCicloDeVida(actividad: Actividad): Boolean {
        val ahora = DateTimeUtils.ahoraEpochMillis()
        return when (val detalle = actividad.detalle) {
            is ActividadDetalle.Cita -> detalle.fechaHora < ahora
            is ActividadDetalle.Medicamento -> detalle.fechaFin != null && detalle.fechaFin < ahora
            else -> false
        }
    }
}
