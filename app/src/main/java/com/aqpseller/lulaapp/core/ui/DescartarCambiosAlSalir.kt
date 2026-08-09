package com.aqpseller.lulaapp.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Intercepta el botón/gesto de retroceso cuando hay contenido sin guardar en una pantalla
 * "Crear X" — sin esto, tocar "Listo" en un `ModalBottomSheet` (que solo cierra el sheet, no
 * guarda nada) puede confundir a la persona pensando que ya guardó, y salir de la pantalla sin
 * tocar el botón final "Crear"/"Guardar cambios" pierde todo lo escrito sin ningún aviso. Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
@Composable
fun DescartarCambiosAlSalir(hayContenidoSinGuardar: Boolean, onDescartar: () -> Unit) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    BackHandler(enabled = hayContenidoSinGuardar) { mostrarConfirmacion = true }
    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Todavía no guardaste — si sales ahora, se pierde lo que escribiste.") },
            confirmButton = { TextButton(onClick = onDescartar) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { mostrarConfirmacion = false }) { Text("Seguir editando") } },
        )
    }
}
