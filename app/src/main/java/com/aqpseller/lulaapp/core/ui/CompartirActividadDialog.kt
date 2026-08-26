package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.domain.model.PermisoCompartir

private fun etiquetaPermiso(permiso: PermisoCompartir): String = when (permiso) {
    PermisoCompartir.PUEDE_VER -> "Solo ver el seguimiento"
    PermisoCompartir.PUEDE_VER_Y_RECORDAR -> "Ver y recordarme"
}

/**
 * "Compartir seguimiento" desde el detalle de cualquier elemento (Círculo de cuidado). Dos
 * caminos, igual que Familia (`FamiliaScreen`): **QR** (primario, sin escribir nada — acompaña
 * al instante en cuanto la otra persona escanea, sin esperar "aceptar") o **correo/teléfono en
 * texto**, para alguien que no está físicamente presente ahora — esa queda `PENDIENTE` hasta
 * que la otra persona la acepte de verdad. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@Composable
fun CompartirActividadDialog(
    nombreActividad: String,
    onElegirQr: (permiso: PermisoCompartir) -> Unit,
    onEnviar: (contacto: String, permiso: PermisoCompartir) -> Unit,
    onCancelar: () -> Unit,
    /** Meta todavía no tiene el contenido real conectado a "Lo que me comparten" (vive en su
     * propia tabla, no en `Actividad`) — ofrecer QR ahí prometería un "✅ confirmado" que nunca
     * muestra nada del otro lado. Solo Meta pasa `false`. Ver `Plan/10-pendientes.md`. */
    soportaQr: Boolean = true,
) {
    var contacto by remember { mutableStateOf("") }
    var permiso by remember { mutableStateOf(PermisoCompartir.PUEDE_VER) }
    var mostrarFormularioContacto by remember { mutableStateOf(!soportaQr) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Compartir \"$nombreActividad\"") },
        text = {
            Column {
                Text("¿Qué puede hacer esta persona?")
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    PermisoCompartir.entries.forEach { opcion ->
                        FilterChip(
                            selected = permiso == opcion,
                            onClick = { permiso = opcion },
                            label = { Text(etiquetaPermiso(opcion)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }

                if (!mostrarFormularioContacto && soportaQr) {
                    Button(onClick = { onElegirQr(permiso) }, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Que te acompañen escaneando")
                    }
                    Text(
                        text = "Sin escribir nada — queda acompañándote apenas escanea con el botón de escanear de su Lula.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    OutlinedButton(onClick = { mostrarFormularioContacto = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Invitar por correo o teléfono")
                    }
                } else {
                    Text("Con quién (nombre, correo o teléfono):", modifier = Modifier.padding(top = 16.dp))
                    OutlinedTextField(
                        value = contacto,
                        onValueChange = { contacto = it },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (mostrarFormularioContacto) {
                TextButton(
                    onClick = { if (contacto.isNotBlank()) onEnviar(contacto, permiso) },
                ) { Text("Enviar solicitud") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
