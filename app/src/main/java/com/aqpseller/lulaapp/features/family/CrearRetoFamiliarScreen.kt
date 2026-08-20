package com.aqpseller.lulaapp.features.family

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.aqpseller.lulaapp.domain.model.FrecuenciaRetoFamiliar

private fun etiquetaFrecuencia(frecuencia: FrecuenciaRetoFamiliar): String = when (frecuencia) {
    FrecuenciaRetoFamiliar.DIARIA -> "Diaria"
    FrecuenciaRetoFamiliar.SEMANAL -> "Semanal"
}

@Composable
fun CrearRetoFamiliarScreen(
    onGuardado: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearRetoFamiliarViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf(FrecuenciaRetoFamiliar.DIARIA) }
    var recompensa by remember { mutableStateOf("") }

    val context = LocalContext.current
    val miembros by viewModel.miembros.collectAsState()
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
        hayContenidoSinGuardar = nombre.isNotBlank() && !guardado,
        onDescartar = onSalirSinGuardar,
    )

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Crear reto familiar", style = MaterialTheme.typography.titleLarge)

        DictationTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre", modifier = Modifier.padding(top = 16.dp))
        DictationTextField(value = objetivo, onValueChange = { objetivo = it }, label = "Objetivo", modifier = Modifier.padding(top = 16.dp))

        Text(text = "Frecuencia", modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            FrecuenciaRetoFamiliar.entries.forEach { opcion ->
                FilterChip(
                    selected = frecuencia == opcion,
                    onClick = { frecuencia = opcion },
                    label = { Text(etiquetaFrecuencia(opcion)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        DictationTextField(
            value = recompensa,
            onValueChange = { recompensa = it },
            label = "Recompensa (opcional)",
            modifier = Modifier.padding(top = 16.dp),
        )

        if (miembros.size > 1) {
            Text(text = "Participantes", modifier = Modifier.padding(top = 16.dp))
            miembros.forEach { miembro ->
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = miembro.seleccionado, onCheckedChange = { viewModel.alternarSeleccion(miembro.usuarioId) })
                    Text(text = miembro.nombre)
                }
            }
        }

        Button(
            onClick = { viewModel.guardar(nombre, objetivo, frecuencia, recompensa) },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text("Crear reto")
        }
    }
}
