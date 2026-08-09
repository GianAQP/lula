package com.aqpseller.lulaapp.core.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.core.utils.QrCodeGenerator
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
    val context = LocalContext.current
    val texto = remember(nombreActividad, permiso) { textoInvitacion(nombreActividad, permiso) }
    val qr = remember(texto) { QrCodeGenerator.generar(texto) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Solicitud enviada") },
        text = {
            Column {
                Text(
                    text = "Todavía es \"Pendiente\" hasta que la otra persona instale Lula y acepte " +
                        "— podés verla en \"Mi círculo de cuidado\". Mientras tanto, avísale así:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (qr != null) {
                    Image(
                        bitmap = qr,
                        contentDescription = "Código QR con la invitación",
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .size(200.dp),
                    )
                    Text(
                        text = "Que la otra persona escanee este código con la cámara del teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, texto)
                }
                runCatching { context.startActivity(Intent.createChooser(intent, "Compartir invitación")) }
            }) {
                Text("📤 Enviar por WhatsApp u otra app")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        },
    )
}
