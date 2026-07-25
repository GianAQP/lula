package com.aqpseller.lulaapp.features.finances

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
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearMovimientoScreen(
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CrearMovimientoViewModel = hiltViewModel(),
) {
    var tipo by remember { mutableStateOf(viewModel.tipoInicial) }
    var montoTexto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    val guardado by viewModel.guardado.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Registrar movimiento", style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.padding(top = 16.dp)) {
            FilterChip(
                selected = tipo == TipoMovimientoFinanciero.EGRESO,
                onClick = { tipo = TipoMovimientoFinanciero.EGRESO },
                label = { Text("Gasto") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = tipo == TipoMovimientoFinanciero.INGRESO,
                onClick = { tipo = TipoMovimientoFinanciero.INGRESO },
                label = { Text("Ingreso") },
            )
        }

        OutlinedTextField(
            value = montoTexto,
            onValueChange = { nuevoValor -> if (nuevoValor.all { it.isDigit() || it == '.' }) montoTexto = nuevoValor },
            label = { Text("Monto (S/)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        OutlinedTextField(
            value = categoria,
            onValueChange = { categoria = it },
            label = { Text("Categoría") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción (opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        Button(
            onClick = {
                viewModel.guardar(tipo, montoTexto.toDoubleOrNull() ?: 0.0, categoria, descripcion.ifBlank { null })
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Guardar")
        }
    }
}
