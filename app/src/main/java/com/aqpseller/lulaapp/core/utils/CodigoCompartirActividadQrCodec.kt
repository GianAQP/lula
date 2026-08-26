package com.aqpseller.lulaapp.core.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Código de "Compartir seguimiento" con tiempo de vida corto (ver
 * `CompartirSyncRepository.generarCodigoCompartir`) — igual que `CodigoEspacioQrPayload` de
 * Familia, escanear este código acompaña de inmediato, sin un paso de aceptar aparte. El QR solo
 * lleva el id del código; los datos reales viven en Firestore y se validan ahí (vencimiento, ya
 * reclamado) antes de crear la solicitud. Ver `Plan/08-decisiones-tecnicas.md`. */
@Serializable
data class CodigoCompartirActividadQrPayload(val codigoId: String)

private const val PREFIJO = "LULA_CODIGO_COMPARTIR_V1:"
private val json = Json { ignoreUnknownKeys = true }

fun codificarCodigoCompartirQr(codigoId: String): String =
    PREFIJO + json.encodeToString(CodigoCompartirActividadQrPayload(codigoId))

fun decodificarCodigoCompartirQr(texto: String): CodigoCompartirActividadQrPayload? {
    if (!texto.startsWith(PREFIJO)) return null
    return runCatching { json.decodeFromString<CodigoCompartirActividadQrPayload>(texto.removePrefix(PREFIJO)) }.getOrNull()
}
