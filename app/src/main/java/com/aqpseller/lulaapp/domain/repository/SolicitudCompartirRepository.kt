package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import kotlinx.coroutines.flow.Flow

interface SolicitudCompartirRepository {
    suspend fun crear(solicitud: SolicitudCompartir, usuarioId: String)

    /**
     * Cancela una solicitud enviada. Hoy solo puede estar `PENDIENTE` (nunca se aceptó de
     * verdad, ver `SolicitudCompartir`) así que basta con eliminarla — cuando exista
     * aceptación real entre cuentas, cancelar una ya `ACEPTADA` también deberá quitar a
     * `para` de `puedeVer[]`/`puedeRecordar[]` de la actividad.
     */
    suspend fun cancelar(solicitudId: String, usuarioId: String)

    fun observarEnviadasPor(usuarioId: String): Flow<List<SolicitudCompartir>>

    /**
     * Solicitudes que alguien más me envió a mí, todavía sin responder — hoy siempre vacío en
     * un solo dispositivo: `para` guarda un contacto (correo/teléfono) de texto libre, no un
     * `usuarioId` real, porque no existe ninguna cuenta ni servidor que empareje ambos lados.
     * Queda conectado igual para activarse solo en cuanto eso exista (ver
     * `Plan/08-decisiones-tecnicas.md`).
     */
    fun observarPendientesPara(usuarioId: String): Flow<List<SolicitudCompartir>>
}
