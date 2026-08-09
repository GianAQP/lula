package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * "Sin recordatorio" + un chip que abre un `TimePicker` real (elegís cualquier hora, no solo
 * presets) — reemplaza los chips de hora fija. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoraRecordatorioSelector(
    horaSeleccionada: String?,
    onHoraSeleccionada: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarSelector by remember { mutableStateOf(false) }

    Row(modifier = modifier) {
        FilterChip(
            selected = horaSeleccionada == null,
            onClick = { onHoraSeleccionada(null) },
            label = { Text("Sin recordatorio") },
            modifier = Modifier.padding(end = 8.dp),
        )
        FilterChip(
            selected = horaSeleccionada != null,
            onClick = { mostrarSelector = true },
            label = { Text(horaSeleccionada ?: "Elegir hora") },
        )
    }

    if (mostrarSelector) {
        val (horaInicial, minutoInicial) = horaSeleccionada
            ?.split(":")
            ?.let { partes -> (partes.getOrNull(0)?.toIntOrNull() ?: 8) to (partes.getOrNull(1)?.toIntOrNull() ?: 0) }
            ?: (8 to 0)
        val estado = rememberTimePickerState(initialHour = horaInicial, initialMinute = minutoInicial, is24Hour = true)

        Dialog(onDismissRequest = { mostrarSelector = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "¿A qué hora te recuerdo?", style = MaterialTheme.typography.titleMedium)
                    TimePicker(state = estado, modifier = Modifier.padding(top = 16.dp))
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { mostrarSelector = false }) { Text("Cancelar") }
                        TextButton(onClick = {
                            val hora = estado.hour.toString().padStart(2, '0')
                            val minuto = estado.minute.toString().padStart(2, '0')
                            onHoraSeleccionada("$hora:$minuto")
                            mostrarSelector = false
                        }) { Text("Confirmar") }
                    }
                }
            }
        }
    }
}
