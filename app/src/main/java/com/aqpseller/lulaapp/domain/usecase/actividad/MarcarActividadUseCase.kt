package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class MarcarActividadUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(actividadId: String, estado: EstadoActividad, usuarioId: String) {
        actividadRepository.marcarEstado(actividadId, estado, usuarioId)
    }
}
