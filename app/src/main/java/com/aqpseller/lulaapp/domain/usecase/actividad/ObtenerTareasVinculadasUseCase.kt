package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

/** Tareas que acompañan a un Medicamento/Cita — ver `ActividadDetalle.Tarea.actividadVinculadaId`. */
class ObtenerTareasVinculadasUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(actividadVinculadaId: String): List<Actividad> =
        actividadRepository.obtenerTareasVinculadasA(actividadVinculadaId)
}
