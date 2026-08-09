package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class ObtenerDetalleActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(actividadId: String): Actividad? = actividadRepository.obtenerConDetalle(actividadId)
}
