package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.Privacidad
import com.aqpseller.lulaapp.domain.model.SyncStatus
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class CrearRutinaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        propietario: String,
        nombre: String,
        momentoDelDia: MomentoDelDia,
        actividadesIncluidasIds: List<String>,
    ) {
        val id = IdGenerator.newId()
        val actividad = Actividad(
            id = id,
            tipo = TipoActividad.RUTINA,
            espacioId = espacioId,
            nombre = nombre,
            propietario = propietario,
            responsables = listOf(propietario),
            puedeVer = emptyList(),
            puedeRecordar = emptyList(),
            estado = EstadoActividad.SIN_CONFIRMAR,
            privacidad = Privacidad.SOLO_YO,
            syncStatus = SyncStatus.LOCAL,
            esPremiumFeature = false,
            areaDeVidaId = null,
            momentoDelDia = momentoDelDia,
            fechaCreacion = DateTimeUtils.ahoraEpochMillis(),
            activa = true,
            detalle = null,
        )
        val detalle = ActividadDetalle.Rutina(
            actividadesIncluidasIds = actividadesIncluidasIds,
            momentoDelDia = momentoDelDia,
        )
        actividadRepository.crearRutina(actividad, detalle, propietario)
    }
}
