package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class ActualizarRutinaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(
        actividadId: String,
        usuarioId: String,
        nombre: String,
        momentoDelDia: MomentoDelDia,
        actividadesIncluidasIds: List<String>,
    ) {
        val detalle = ActividadDetalle.Rutina(
            actividadesIncluidasIds = actividadesIncluidasIds,
            momentoDelDia = momentoDelDia,
        )
        actividadRepository.actualizarRutina(actividadId, nombre, detalle, usuarioId)
    }
}
