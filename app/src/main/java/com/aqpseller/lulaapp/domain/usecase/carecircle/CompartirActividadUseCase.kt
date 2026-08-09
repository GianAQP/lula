package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import javax.inject.Inject

/**
 * Envía una solicitud para compartir el seguimiento de una actividad con alguien — regla no
 * negociable: compartir es **solicitud + aceptación**, nunca automático (ver
 * `Plan/01-arquitectura.md`). Esta solicitud queda `PENDIENTE`; todavía no hay forma de que
 * la otra persona la acepte de verdad (necesita cuenta propia, ver `08-decisiones-tecnicas.md`),
 * así que **no** modifica `puedeVer[]`/`puedeRecordar[]` de la actividad todavía.
 */
class CompartirActividadUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
) {
    suspend operator fun invoke(
        deUsuarioId: String,
        actividadId: String,
        nombreActividad: String,
        contactoDestinatario: String,
        permiso: PermisoCompartir,
    ) {
        val solicitud = SolicitudCompartir(
            id = IdGenerator.newId(),
            de = deUsuarioId,
            para = contactoDestinatario,
            tieneCuenta = false,
            elementoId = actividadId,
            contexto = nombreActividad,
            permisos = permiso,
            estado = EstadoSolicitud.PENDIENTE,
            canalEnvio = null,
            fechaSolicitud = DateTimeUtils.ahoraEpochMillis(),
        )
        solicitudCompartirRepository.crear(solicitud, deUsuarioId)
    }
}
