package com.aqpseller.lulaapp.features.lists

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.DescartarCambiosAlSalir
import com.aqpseller.lulaapp.core.ui.DictationTextField

@Composable
fun CrearListaScreen(
    onGuardado: () -> Unit,
    onVerListas: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearListaViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf("")) }

    val context = LocalContext.current
    val guardado by viewModel.guardado.collectAsState()
    val mensajeError by viewModel.mensajeError.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.errorMostrado()
        }
    }
    DescartarCambiosAlSalir(
        hayContenidoSinGuardar = (nombre.isNotBlank() || items.any { it.isNotBlank() }) && !guardado,
        onDescartar = onSalirSinGuardar,
    )

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Nueva lista", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onVerListas, modifier = Modifier.padding(top = 4.dp)) {
            Text("📋 Ver mis listas")
        }
        Text(
            text = "Para cosas que preparas de vez en cuando (ej. \"Viaje\", \"Compras del mes\") — " +
                "cada vez que la uses, desmárcala de nuevo con \"Reiniciar lista\".",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )

        DictationTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre de la lista",
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(text = "Ítems", modifier = Modifier.padding(top = 16.dp))
        items.forEachIndexed { index, texto ->
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                DictationTextField(
                    value = texto,
                    onValueChange = { nuevo -> items = items.toMutableList().also { it[index] = nuevo } },
                    label = "Ítem ${index + 1}",
                    modifier = Modifier.weight(1f),
                )
                if (items.size > 1) {
                    IconButton(onClick = { items = items.toMutableList().also { it.removeAt(index) } }) {
                        Text("✕")
                    }
                }
            }
        }
        OutlinedButton(onClick = { items = items + "" }, modifier = Modifier.padding(top = 8.dp)) {
            Text("+ Agregar ítem")
        }

        Button(
            onClick = { viewModel.guardar(nombre, items) },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Crear lista")
        }
    }
}
