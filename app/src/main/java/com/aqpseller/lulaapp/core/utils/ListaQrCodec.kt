package com.aqpseller.lulaapp.core.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Copia de una Lista vía QR — transferencia de una sola vez, sin quedar vinculadas después (a
 * diferencia de `SolicitudCompartir`, que sí sincroniza). No necesita backend: todo el contenido
 * vive dentro del propio QR. Ver `Plan/10-pendientes.md`.
 */
@Serializable
data class ListaQrPayload(val nombre: String, val items: List<String>)

private const val PREFIJO = "LULA_LISTA_V1:"
private val json = Json { ignoreUnknownKeys = true }

fun codificarListaQr(nombre: String, items: List<String>): String =
    PREFIJO + json.encodeToString(ListaQrPayload(nombre, items))

fun decodificarListaQr(texto: String): ListaQrPayload? {
    if (!texto.startsWith(PREFIJO)) return null
    return runCatching { json.decodeFromString<ListaQrPayload>(texto.removePrefix(PREFIJO)) }.getOrNull()
}
