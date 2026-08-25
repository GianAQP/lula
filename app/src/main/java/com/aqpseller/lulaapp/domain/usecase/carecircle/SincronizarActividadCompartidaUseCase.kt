package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoSolicitud
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.CareCircleContenidoSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Sube (o vuelve a subir) el contenido real de una actividad que ya comparto con alguien —
 * a diferencia de `CompartirActividadUseCase`/`AceptarSolicitudCompartirUseCase` (la "capa
 * social": quién pidió, quién aceptó), esto es lo que la otra persona realmente ve. Solo tiene
 * efecto si la solicitud es de tipo ACTIVIDAD y ya está ACEPTADA — no-op en cualquier otro caso.
 * Ver `Plan/08-decisiones-tecnicas.md`.
 */
class SincronizarActividadCompartidaUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
    private val careCircleContenidoSyncRepository: CareCircleContenidoSyncRepository,
) {
    suspend operator fun invoke(solicitud: SolicitudCompartir) {
        if (solicitud.tipo != TipoSolicitud.ACTIVIDAD || solicitud.estado != EstadoSolicitud.ACEPTADA) return
        val actividad = actividadRepository.obtenerConDetalle(solicitud.elementoId) ?: return
        val historial = if (actividad.tipo == TipoActividad.HABITO) {
            actividadRepository.obtenerHistorialHabito(actividad.id, 14)
        } else {
            emptyList()
        }
        val tomas = if (actividad.tipo == TipoActividad.MEDICAMENTO) {
            actividadRepository.obtenerHistorialTomas(actividad.id, 7)
        } else {
            emptyList()
        }
        val sesiones = if (actividad.tipo == TipoActividad.CITA) {
            actividadRepository.obtenerSesionesCita(actividad.id)
        } else {
            emptyList()
        }
        careCircleContenidoSyncRepository.subirActividadCompartida(
            solicitudId = solicitud.id,
            paraCorreo = solicitud.para,
            deNombre = solicitud.deNombre,
            permiso = solicitud.permisos,
            actividad = actividad,
            detalle = actividad.detalle,
            historialHabito = historial,
            tomasRecientes = tomas,
            sesionesCita = sesiones,
        )
    }
}

/** Punto de entrada desde los "Marcar" / "Actualizar" de cada tipo — resuelve solo si
 * [actividadId] tiene alguna solicitud ACEPTADA saliente, y si la tiene, la resube. Pensado para
 * llamarse con `runCatching` después de cualquier cambio real en la actividad (marcar hecho,
 * marcar toma, etc.). */
class SincronizarSiEstaCompartidaUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
    private val sincronizarActividadCompartidaUseCase: SincronizarActividadCompartidaUseCase,
) {
    suspend operator fun invoke(actividadId: String, usuarioId: String) {
        solicitudCompartirRepository.observarEnviadasPor(usuarioId).first()
            .filter { it.tipo == TipoSolicitud.ACTIVIDAD && it.estado == EstadoSolicitud.ACEPTADA && it.elementoId == actividadId }
            .forEach { solicitud -> sincronizarActividadCompartidaUseCase(solicitud) }
    }
}

/** Contraparte de borrado — se llama al eliminar una actividad que tenía compartidos activos,
 * para que quien la acompañaba no se quede viendo algo huérfano en "Lo que me comparten". */
class EliminarActividadesCompartidasDeUseCase @Inject constructor(
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
    private val careCircleContenidoSyncRepository: CareCircleContenidoSyncRepository,
) {
    suspend operator fun invoke(actividadId: String, usuarioId: String) {
        solicitudCompartirRepository.observarEnviadasPor(usuarioId).first()
            .filter { it.tipo == TipoSolicitud.ACTIVIDAD && it.elementoId == actividadId }
            .forEach { solicitud -> careCircleContenidoSyncRepository.eliminarActividadCompartida(solicitud.id) }
    }
}
