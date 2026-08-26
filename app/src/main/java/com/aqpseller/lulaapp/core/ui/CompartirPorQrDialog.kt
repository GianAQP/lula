package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.core.utils.QrCodeGenerator

/**
 * Muestra el QR de "Compartir seguimiento" y reacciona en vivo cuando alguien lo escanea — a
 * diferencia de `InvitacionQrDialog` (texto de invitación fijo, para WhatsApp), este código es
 * accionable: escanearlo con el botón de escanear (ícono `QrCodeScanner`) de la barra superior
 * acompaña de inmediato. Ver `CompartirPorQrController`.
 */
@Composable
fun CompartirPorQrDialog(
    nombreActividad: String,
    estado: CompartirQrEstado,
    onReintentar: () -> Unit,
    onCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Que te acompañen en \"$nombreActividad\"") },
        text = {
            Column {
                when (estado) {
                    CompartirQrEstado.Oculto -> Unit
                    CompartirQrEstado.Generando -> {
                        Text("Generando código…", style = MaterialTheme.typography.bodySmall)
                        CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
                    }
                    is CompartirQrEstado.Mostrando -> {
                        Text(
                            text = "Con el botón de escanear de su Lula, apuntando la cámara acá — queda " +
                                "acompañándote al instante, sin escribir nada. Este código dura 3 minutos.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val qr = remember(estado.qrTexto) { QrCodeGenerator.generar(estado.qrTexto) }
                        if (qr != null) {
                            Image(
                                bitmap = qr,
                                contentDescription = "Código QR para compartir seguimiento",
                                modifier = Modifier.padding(top = 16.dp).size(220.dp),
                            )
                        }
                    }
                    is CompartirQrEstado.Confirmado -> {
                        Text(
                            text = "✅ ¡Listo! Ahora ${estado.nombrePersona} te acompaña en \"$nombreActividad\".",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    CompartirQrEstado.Vencido -> {
                        Text(
                            text = "Este código venció sin que nadie lo escaneara. Genera uno nuevo.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    CompartirQrEstado.SinCuentaVinculada -> {
                        Text(
                            text = "Necesitas vincular tu cuenta con Google primero (Perfil → \"🔑 Cuenta\").",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (estado == CompartirQrEstado.Vencido) {
                TextButton(onClick = onReintentar) { Text("Generar de nuevo") }
            } else {
                TextButton(onClick = onCerrar) { Text("Listo") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        },
    )
}
