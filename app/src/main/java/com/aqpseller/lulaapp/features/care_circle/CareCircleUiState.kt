package com.aqpseller.lulaapp.features.care_circle

import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.TipoSolicitud

data class SolicitudEnviadaUi(
    val id: String,
    val contacto: String,
    val elemento: String,
    val tipo: TipoSolicitud,
    val permiso: PermisoCompartir,
    val estado: EstadoSolicitud,
)

data class SolicitudRecibidaUi(
    val id: String,
    val deNombre: String,
    val elemento: String,
    val tipo: TipoSolicitud,
    val permiso: PermisoCompartir,
)

data class CareCircleUiState(
    val cargando: Boolean = true,
    val enviadas: List<SolicitudEnviadaUi> = emptyList(),
    val recibidas: List<SolicitudRecibidaUi> = emptyList(),
)
