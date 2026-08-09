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
 * Un solo chip de hora obligatoria (a diferencia de `HoraRecordatorioSelector`, que ofrece
 * "sin recordatorio") — para horarios que sí o sí necesitan un valor: primera dosis de un
 * medicamento, horario de una comida, hora de una cita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoraSelector(
    hora: String?,
    onHoraSeleccionada: (String) -> Unit,
    etiqueta: String = "Elegir hora",
    modifier: Modifier = Modifier,
) {
    var mostrarSelector by remember { mutableStateOf(false) }

    FilterChip(
        selected = hora != null,
        onClick = { mostrarSelector = true },
        label = { Text(hora ?: etiqueta) },
        modifier = modifier,
    )

    if (mostrarSelector) {
        val (horaInicial, minutoInicial) = hora
            ?.split(":")
            ?.let { partes -> (partes.getOrNull(0)?.toIntOrNull() ?: 8) to (partes.getOrNull(1)?.toIntOrNull() ?: 0) }
            ?: (8 to 0)
        val estado = rememberTimePickerState(initialHour = horaInicial, initialMinute = minutoInicial, is24Hour = true)

        Dialog(onDismissRequest = { mostrarSelector = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = etiqueta, style = MaterialTheme.typography.titleMedium)
                    TimePicker(state = estado, modifier = Modifier.padding(top = 16.dp))
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { mostrarSelector = false }) { Text("Cancelar") }
                        TextButton(onClick = {
                            val h = estado.hour.toString().padStart(2, '0')
                            val m = estado.minute.toString().padStart(2, '0')
                            onHoraSeleccionada("$h:$m")
                            mostrarSelector = false
                        }) { Text("Confirmar") }
                    }
                }
            }
        }
    }
}
