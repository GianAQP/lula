package com.aqpseller.lulaapp.features.habits

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
import com.aqpseller.lulaapp.domain.model.MomentoDelDia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearHabitoScreen(
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrearHabitoViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var momento by remember { mutableStateOf(MomentoDelDia.MANANA) }
    var duracionTexto by remember { mutableStateOf("") }

    val guardado by viewModel.guardado.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Nuevo hábito", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        Text(text = "Momento del día", modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            MomentoDelDia.entries.forEach { opcion ->
                FilterChip(
                    selected = momento == opcion,
                    onClick = { momento = opcion },
                    label = { Text(opcion.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        OutlinedTextField(
            value = duracionTexto,
            onValueChange = { nuevoValor -> if (nuevoValor.all { it.isDigit() }) duracionTexto = nuevoValor },
            label = { Text("Duración inicial (opcional, min)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        Button(
            onClick = { viewModel.guardar(nombre, momento, duracionTexto.toIntOrNull()) },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Crear")
        }
    }
}
