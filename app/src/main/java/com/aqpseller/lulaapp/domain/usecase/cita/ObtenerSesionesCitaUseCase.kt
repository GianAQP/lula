package com.aqpseller.lulaapp.domain.usecase.cita

import com.aqpseller.lulaapp.domain.model.SesionCita
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class ObtenerSesionesCitaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(actividadId: String): List<SesionCita> =
        actividadRepository.obtenerSesionesCita(actividadId)
}
