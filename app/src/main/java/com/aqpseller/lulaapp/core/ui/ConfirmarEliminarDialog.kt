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
    /** Mismo diálogo, reutilizado también para otras acciones que merecen una pregunta de
     * seguridad antes de un solo toque (ej. "Hacer admin"/"Quitar admin" en Familia) — no todas
     * son una eliminación, así que el título/texto del botón se pueden personalizar. Por
     * defecto sigue igual que siempre. */
    titulo: String = "¿Eliminar?",
    textoConfirmar: String = "Eliminar",
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(textoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
