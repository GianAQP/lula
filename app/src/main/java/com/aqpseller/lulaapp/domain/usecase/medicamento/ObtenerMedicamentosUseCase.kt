package com.aqpseller.lulaapp.domain.usecase.medicamento

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerMedicamentosUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<Actividad>> = actividadRepository.observarMedicamentos(espacioId)
}
