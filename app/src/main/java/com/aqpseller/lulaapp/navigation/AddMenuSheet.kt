package com.aqpseller.lulaapp.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.core.ui.ColoredEmojiIcon
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaFinanzasContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaHabitoContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaTareaContainerLight

private data class OpcionMenu(val nombre: String, val emoji: String, val color: Color)

private val OPCIONES_DISPONIBLES = listOf(
    OpcionMenu("Hábito", "✅", LulaHabitoContainerLight),
    OpcionMenu("Tarea", "📝", LulaTareaContainerLight),
    OpcionMenu("Rutina", "🧩", LulaPrimaryContainerLight),
    OpcionMenu("Meta", "🎯", LulaPrimaryContainerLight),
    OpcionMenu("Lista", "📋", LulaPrimaryContainerLight),
    OpcionMenu("Gasto", "📉", LulaRachaContainerLight),
    OpcionMenu("Ingreso", "📈", LulaFinanzasContainerLight),
    OpcionMenu("Nota", "🗒️", LulaPrimaryContainerLight),
    OpcionMenu("Medicamento", "💊", LulaAsistenteContainerLight),
    OpcionMenu("Cita", "📅", LulaPrimaryContainerLight),
    OpcionMenu("Fecha importante", "🎉", LulaAsistenteContainerLight),
    OpcionMenu("Diario", "📓", LulaPrimaryContainerLight),
)

/** Menú `+` de `Plan/02-pantallas.md`. Todas las opciones tienen un flujo real. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuSheet(
    onDismiss: () -> Unit,
    onOpcionSeleccionada: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = "¿Qué quieres agregar?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OPCIONES_DISPONIBLES.forEach { opcion ->
                FilaOpcionMenu(
                    emoji = opcion.emoji,
                    color = opcion.color,
                    texto = opcion.nombre,
                    onClick = { onOpcionSeleccionada(opcion.nombre) },
                )
            }
            FilaOpcionMenu(
                emoji = "🎙️",
                color = LulaAsistenteContainerLight,
                texto = "Hablar con Lula",
                onClick = { onOpcionSeleccionada("Hablar con Lula") },
            )
        }
    }
}

@Composable
private fun FilaOpcionMenu(emoji: String, color: Color, texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColoredEmojiIcon(emoji = emoji, colorContenedor = color, tamano = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}
