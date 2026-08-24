package com.aqpseller.lulaapp.core.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Código de invitación a un Espacio Familia, con tiempo de vida corto (ver
 * `EspacioSyncRepository.generarCodigoInvitacion`) — a diferencia de `ContactoQrPayload`,
 * escanear este código une a la persona al espacio de inmediato, sin un paso de aceptar aparte.
 * El QR solo lleva el id del código; los datos reales viven en Firestore y se validan ahí
 * (vencimiento, ya reclamado) antes de unir a nadie. Ver `Plan/08-decisiones-tecnicas.md`. */
@Serializable
data class CodigoEspacioQrPayload(val codigoId: String)

private const val PREFIJO = "LULA_CODIGO_ESPACIO_V1:"
private val json = Json { ignoreUnknownKeys = true }

fun codificarCodigoEspacioQr(codigoId: String): String =
    PREFIJO + json.encodeToString(CodigoEspacioQrPayload(codigoId))

fun decodificarCodigoEspacioQr(texto: String): CodigoEspacioQrPayload? {
    if (!texto.startsWith(PREFIJO)) return null
    return runCatching { json.decodeFromString<CodigoEspacioQrPayload>(texto.removePrefix(PREFIJO)) }.getOrNull()
}
