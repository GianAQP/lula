package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import kotlinx.coroutines.flow.Flow

interface SolicitudCompartirRepository {
    /** También sirve para "recibir" una solicitud que llegó por Firestore (upsert por id) —
     * ver `Plan/12-firebase-auth-y-sync.md`. */
    suspend fun crear(solicitud: SolicitudCompartir, usuarioId: String)

    /**
     * Cancela una solicitud enviada. Hoy solo puede estar `PENDIENTE` (nunca se aceptó de
     * verdad, ver `SolicitudCompartir`) así que basta con eliminarla — cuando exista
     * aceptación real entre cuentas, cancelar una ya `ACEPTADA` también deberá quitar a
     * `para` de `puedeVer[]`/`puedeRecordar[]` de la actividad.
     */
    suspend fun cancelar(solicitudId: String, usuarioId: String)

    /** Acepta o rechaza una solicitud que me enviaron — marca `estado` y `fechaRespuesta`. */
    suspend fun responder(solicitudId: String, estado: EstadoSolicitud, usuarioId: String)

    fun observarEnviadasPor(usuarioId: String): Flow<List<SolicitudCompartir>>

    suspend fun obtenerPorId(solicitudId: String): SolicitudCompartir?

    /**
     * Solicitudes que alguien más me envió a mí, todavía sin responder — filtra por mi correo
     * (`para` es un contacto de texto libre, no un `usuarioId`). Vacío hasta que la cuenta esté
     * vinculada con Google. Ver `Plan/12-firebase-auth-y-sync.md`.
     */
    fun observarPendientesPara(correo: String): Flow<List<SolicitudCompartir>>
}
