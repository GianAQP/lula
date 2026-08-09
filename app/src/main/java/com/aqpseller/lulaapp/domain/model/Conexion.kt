package com.aqpseller.lulaapp.domain.model

enum class TipoConexion { FAMILIA, AMIGO, CUIDADOR }

/**
 * Recuerda que dos personas ya se conectaron, después de aceptar una `SolicitudCompartir` —
 * a diferencia de esa tabla (que es sobre un elemento puntual compartido), esta es la relación
 * persistente entre ambas. Ver `Plan/11-cuentas-y-conexiones.md`. Bloqueada por backend igual
 * que el resto de compartir (necesita una segunda persona real) — el modelo queda listo,
 * todavía sin ningún punto del código que la cree de verdad.
 */
data class Conexion(
    val id: String,
    val usuarioA: String,
    val usuarioB: String,
    val tipo: TipoConexion,
    val origenSolicitudId: String?,
    val fechaConexion: Long,
)
