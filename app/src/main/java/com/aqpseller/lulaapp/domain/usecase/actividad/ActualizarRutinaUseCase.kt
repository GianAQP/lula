package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class ActualizarRutinaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val espacioRepository: EspacioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
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
        val actividad = actividadRepository.obtenerConDetalle(actividadId) ?: return
        if (espacioRepository.obtenerEspacioSiEsMiembro(actividad.espacioId, usuarioId)?.tipo == TipoEspacio.PERSONAL) {
            runCatching { personalSyncRepository.subirRutina(actividad, detalle) }
        }
    }
}
