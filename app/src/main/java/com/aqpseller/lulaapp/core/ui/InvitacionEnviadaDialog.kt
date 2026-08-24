package com.aqpseller.lulaapp.core.ui

import androidx.compose.runtime.Composable
import com.aqpseller.lulaapp.domain.model.PermisoCompartir

private fun etiquetaPermisoInvitacion(permiso: PermisoCompartir): String = when (permiso) {
    PermisoCompartir.PUEDE_VER -> "ver tu seguimiento"
    PermisoCompartir.PUEDE_VER_Y_RECORDAR -> "ver tu seguimiento y recordarte"
}

private fun textoInvitacion(nombreActividad: String, permiso: PermisoCompartir): String =
    "🌱 Te quiero compartir el seguimiento de \"$nombreActividad\" en Lula — vas a poder " +
        "${etiquetaPermisoInvitacion(permiso)}. La app todavía no está en la tienda; " +
        "escríbeme y coordinamos cómo instalarla."

/**
 * Aparece después de crear una `SolicitudCompartir` — ofrece las dos formas "modernas" de
 * pasarle la invitación a la otra persona: un código QR generado en el momento (sin red, para
 * cuando están los dos teléfonos juntos) y el selector nativo de Android para enviarla por
 * WhatsApp, Telegram, SMS o cualquier otra app instalada. Buscar directamente entre usuarios
 * que ya tienen Lula, o que escanear el QR complete el acceso solo, necesita una cuenta real y
 * un servidor que sincronice ambos teléfonos — todavía no existen, ver
 * `Plan/08-decisiones-tecnicas.md`. Por ahora esto solo transmite el mensaje de invitación.
 */
@Composable
fun InvitacionEnviadaDialog(
    nombreActividad: String,
    permiso: PermisoCompartir,
    onCerrar: () -> Unit,
) {
    InvitacionQrDialog(
        titulo = "Solicitud enviada",
        mensaje = "Todavía es \"Pendiente\" hasta que la otra persona instale Lula y acepte " +
            "— podés verla en \"Mi círculo de cuidado\". Mientras tanto, avísale así:",
        textoInvitacion = textoInvitacion(nombreActividad, permiso),
        onCerrar = onCerrar,
    )
}
