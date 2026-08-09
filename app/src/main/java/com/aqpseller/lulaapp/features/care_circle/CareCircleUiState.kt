package com.aqpseller.lulaapp.features.care_circle

import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.PermisoCompartir

data class SolicitudEnviadaUi(
    val id: String,
    val contacto: String,
    val elemento: String,
    val permiso: PermisoCompartir,
    val estado: EstadoSolicitud,
)

data class CareCircleUiState(
    val cargando: Boolean = true,
    val enviadas: List<SolicitudEnviadaUi> = emptyList(),
)
