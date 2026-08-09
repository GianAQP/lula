package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class ObtenerHistorialTomasUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(actividadId: String, dias: Int = 7): List<TomaMedicamento> =
        actividadRepository.obtenerHistorialTomas(actividadId, dias)
}
