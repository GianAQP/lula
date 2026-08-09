package com.aqpseller.lulaapp.domain.usecase.fechaimportante

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerFechasImportantesUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<Actividad>> = actividadRepository.observarFechasImportantes(espacioId)
}
