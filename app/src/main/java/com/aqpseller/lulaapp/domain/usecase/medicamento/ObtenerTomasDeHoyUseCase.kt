package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerTomasDeHoyUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    operator fun invoke(actividadIds: List<String>): Flow<List<TomaMedicamento>> =
        actividadRepository.observarTomasDeHoy(actividadIds)
}
