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

/**
 * Igual que `InvitacionEnviadaDialog` (mismo patrón: QR + "Enviar por WhatsApp u otra app") pero
 * genérico — no está atado a compartir una Actividad puntual (`nombreActividad`/`PermisoCompartir`),
 * así que lo puede usar cualquier flujo de invitación (ej. Espacio Familia). El texto ya viene
 * armado por quien llama.
 */
@Composable
fun InvitacionQrDialog(
    titulo: String,
    mensaje: String,
    textoInvitacion: String,
    onCerrar: () -> Unit,
) {
    val context = LocalContext.current
    val qr = remember(textoInvitacion) { QrCodeGenerator.generar(textoInvitacion) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = {
            Column {
                Text(text = mensaje, style = MaterialTheme.typography.bodyMedium)
                if (qr != null) {
                    Image(
                        bitmap = qr,
                        contentDescription = "Código QR con la invitación",
                        modifier = Modifier.padding(top = 16.dp).size(200.dp),
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
                    putExtra(Intent.EXTRA_TEXT, textoInvitacion)
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
