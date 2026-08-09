package com.aqpseller.lulaapp.features.diary

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.utils.DateTimeUtils

/**
 * Como un cuaderno: la fecha es el único encabezado (no hay título ni área de vida — se
 * probó con esos campos y no aportaban, ver `Plan/08-decisiones-tecnicas.md`), y el texto es
 * un solo bloque libre, igual que `NoteEditorScreen`. A propósito **no** usa `verticalScroll` —
 * el campo de texto necesita `weight(1f)` para acotar su altura y scrollear internamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryEditorScreen(
    onGuardado: () -> Unit,
    onEliminada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryEntryEditorViewModel = hiltViewModel(),
) {
    var texto by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(viewModel.fechaPreset ?: DateTimeUtils.inicioDeHoyEpochMillis()) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val estadoInicial by viewModel.estadoInicial.collectAsState()
    val guardado by viewModel.guardado.collectAsState()
    val eliminada by viewModel.eliminada.collectAsState()
    val mensajeError by viewModel.mensajeError.collectAsState()

    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    LaunchedEffect(eliminada) { if (eliminada) onEliminada() }
    LaunchedEffect(estadoInicial) {
        estadoInicial?.let {
            texto = it.texto
            fecha = it.fecha
        }
    }
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.errorMostrado()
        }
    }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            FilterChip(
                selected = true,
                onClick = { mostrarSelectorFecha = true },
                label = { Text(DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fecha))) },
            )
        }

        DictationTextField(
            value = texto,
            onValueChange = { texto = it },
            label = "Escribe lo que quieras",
            singleLine = false,
            modifier = Modifier.weight(1f).padding(top = 8.dp),
        )

        if (viewModel.esEdicion) {
            OutlinedButton(onClick = { mostrarConfirmacion = true }, modifier = Modifier.padding(top = 12.dp)) {
                Text("Eliminar")
            }
        }

        Button(
            onClick = { viewModel.guardar(texto, fecha) },
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    if (mostrarSelectorFecha) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(fecha),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis -> fecha = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis) }
                    mostrarSelectorFecha = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estadoFecha) }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina esta entrada del diario para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminar() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }
}
