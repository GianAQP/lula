package com.aqpseller.lulaapp.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirmación obligatoria antes de cualquier eliminación irreversible en la app — regla
 * general a partir de esta sesión, ver `Plan/08-decisiones-tecnicas.md`. Nada se borra por un
 * solo toque por accidente.
 */
@Composable
fun ConfirmarEliminarDialog(
    mensaje: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Eliminar?") },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Eliminar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
