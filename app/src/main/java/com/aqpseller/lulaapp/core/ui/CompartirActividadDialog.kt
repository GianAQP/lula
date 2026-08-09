package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
 * "Compartir seguimiento" desde el detalle de cualquier elemento — primer paso del flujo de
 * `Plan/02-pantallas.md` (Círculo de cuidado, fase 1.0). Crea una `SolicitudCompartir`
 * `PENDIENTE`; todavía no hay envío real (correo/WhatsApp/SMS) ni aceptación de la otra
 * persona — eso necesita cuentas reales, ver `Plan/08-decisiones-tecnicas.md`.
 */
@Composable
fun CompartirActividadDialog(
    nombreActividad: String,
    onEnviar: (contacto: String, permiso: PermisoCompartir) -> Unit,
    onCancelar: () -> Unit,
) {
    var contacto by remember { mutableStateOf("") }
    var permiso by remember { mutableStateOf(PermisoCompartir.PUEDE_VER) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Compartir \"$nombreActividad\"") },
        text = {
            Column {
                Text("Con quién (nombre, correo o teléfono):")
                OutlinedTextField(
                    value = contacto,
                    onValueChange = { contacto = it },
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text("¿Qué puede hacer esta persona?", modifier = Modifier.padding(top = 16.dp))
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (contacto.isNotBlank()) onEnviar(contacto, permiso) },
            ) { Text("Enviar solicitud") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
