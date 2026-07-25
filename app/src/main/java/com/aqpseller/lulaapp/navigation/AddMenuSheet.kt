package com.aqpseller.lulaapp.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val OPCIONES_DISPONIBLES = listOf(
    "Hábito", "Tarea", "Gasto", "Ingreso", "Nota", "Medicamento", "Cita", "Fecha importante",
)

/** Menú `+` de `Plan/02-pantallas.md`. Solo Hábito/Tarea/Gasto/Ingreso navegan a un flujo real esta sesión. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuSheet(
    onDismiss: () -> Unit,
    onOpcionSeleccionada: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "¿Qué quieres agregar?", style = MaterialTheme.typography.titleMedium)
            OPCIONES_DISPONIBLES.forEach { opcion ->
                TextButton(
                    onClick = { onOpcionSeleccionada(opcion) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = opcion, modifier = Modifier.fillMaxWidth())
                }
            }
            TextButton(
                onClick = { onOpcionSeleccionada("Hablar con Lula") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "🎙️ Hablar con Lula", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
