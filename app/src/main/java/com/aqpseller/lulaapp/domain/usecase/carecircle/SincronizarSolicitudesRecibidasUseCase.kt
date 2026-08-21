package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
import javax.inject.Inject

/**
 * Escucha en vivo Firestore mientras se llame (pensado para correr en el `viewModelScope` de
 * la pantalla de Círculo de cuidado — se cancela solo al cerrarla) y refleja cada cambio en la
 * base local: solicitudes nuevas dirigidas a mí, o respuestas a las que yo envié. Room sigue
 * siendo la única fuente de verdad para la UI — Firestore es solo el transporte. Ver
 * `Plan/12-firebase-auth-y-sync.md`.
 */
class SincronizarSolicitudesRecibidasUseCase @Inject constructor(
    private val compartirSyncRepository: CompartirSyncRepository,
    private val solicitudCompartirRepository: SolicitudCompartirRepository,
) {
    suspend operator fun invoke(miUsuarioId: String, miCorreo: String) {
        compartirSyncRepository.escucharSolicitudes(miUsuarioId, miCorreo).collect { remotas ->
            remotas.forEach { solicitudCompartirRepository.crear(it, miUsuarioId) }
        }
    }
}
