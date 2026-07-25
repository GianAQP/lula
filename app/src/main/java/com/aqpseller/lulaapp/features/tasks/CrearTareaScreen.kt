package com.aqpseller.lulaapp.features.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

private enum class OpcionFecha { SIN_FECHA, HOY, MANANA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearTareaScreen(
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrearTareaViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var opcionFecha by remember { mutableStateOf(OpcionFecha.SIN_FECHA) }
    var importante by remember { mutableStateOf(false) }
    var urgente by remember { mutableStateOf(false) }

    val guardado by viewModel.guardado.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Nueva tarea", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        Text(text = "Fecha límite", modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(
                selected = opcionFecha == OpcionFecha.SIN_FECHA,
                onClick = { opcionFecha = OpcionFecha.SIN_FECHA },
                label = { Text("Sin fecha") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = opcionFecha == OpcionFecha.HOY,
                onClick = { opcionFecha = OpcionFecha.HOY },
                label = { Text("Hoy") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = opcionFecha == OpcionFecha.MANANA,
                onClick = { opcionFecha = OpcionFecha.MANANA },
                label = { Text("Mañana") },
            )
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            FilterChip(
                selected = importante,
                onClick = { importante = !importante },
                label = { Text("Importante") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = urgente,
                onClick = { urgente = !urgente },
                label = { Text("Urgente") },
            )
        }

        Button(
            onClick = {
                val fechaLimite = when (opcionFecha) {
                    OpcionFecha.SIN_FECHA -> null
                    OpcionFecha.HOY -> DateTimeUtils.inicioDeHoyEpochMillis()
                    OpcionFecha.MANANA -> DateTimeUtils.hoy()
                        .plus(DatePeriod(days = 1))
                        .atStartOfDayIn(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds()
                }
                viewModel.guardar(nombre, fechaLimite, importante, urgente)
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Crear")
        }
    }
}
