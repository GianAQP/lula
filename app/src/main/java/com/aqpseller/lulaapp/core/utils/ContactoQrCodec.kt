package com.aqpseller.lulaapp.core.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** "Mi código para conectar" — un QR con mi correo, para que otra persona lo escanee y no tenga
 * que escribirlo a mano al compartir/invitar. No crea nada por sí solo — solo rellena el campo
 * de contacto; el paso de aceptar sigue siendo el mismo de siempre. Ver
 * `Plan/12-firebase-auth-y-sync.md`. */
@Serializable
data class ContactoQrPayload(val correo: String, val nombre: String)

private const val PREFIJO = "LULA_CONTACTO_V1:"
private val json = Json { ignoreUnknownKeys = true }

fun codificarContactoQr(correo: String, nombre: String): String =
    PREFIJO + json.encodeToString(ContactoQrPayload(correo, nombre))

fun decodificarContactoQr(texto: String): ContactoQrPayload? {
    if (!texto.startsWith(PREFIJO)) return null
    return runCatching { json.decodeFromString<ContactoQrPayload>(texto.removePrefix(PREFIJO)) }.getOrNull()
}
