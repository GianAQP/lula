package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Devuelve todas las actividades activas del espacio (hábitos recurrentes + tareas). El
 * agrupado por momento del día y el filtro de "tareas de hoy" se resuelve en el ViewModel,
 * no aquí, para mantener el caso de uso simple y testeable.
 */
class ObtenerActividadesDeHoyUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<Actividad>> =
        actividadRepository.observarActividadesDeEspacio(espacioId)
}
