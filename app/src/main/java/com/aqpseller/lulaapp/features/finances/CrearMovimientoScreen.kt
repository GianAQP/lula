package com.aqpseller.lulaapp.features.finances

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.DescartarCambiosAlSalir
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearMovimientoScreen(
    onGuardado: () -> Unit,
    onVerFinanzas: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearMovimientoViewModel = hiltViewModel(),
) {
    var tipo by remember { mutableStateOf(viewModel.tipoInicial) }
    var montoTexto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<String?>(null) }
    var categoriaPersonalizada by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(DateTimeUtils.inicioDeHoyEpochMillis()) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    fun snapshot() = listOf(tipo, montoTexto, categoria, categoriaPersonalizada, descripcion, fecha)
    var snapshotInicial by remember { mutableStateOf(snapshot()) }

    val guardado by viewModel.guardado.collectAsState()
    val eliminado by viewModel.eliminado.collectAsState()
    val formInicial by viewModel.formInicial.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    LaunchedEffect(eliminado) { if (eliminado) onGuardado() }
    DescartarCambiosAlSalir(
        hayContenidoSinGuardar = snapshotInicial != snapshot() && !guardado && !eliminado,
        onDescartar = onSalirSinGuardar,
    )
    LaunchedEffect(formInicial) {
        formInicial?.let {
            tipo = it.tipo
            montoTexto = it.monto.toString()
            descripcion = it.descripcion.orEmpty()
            fecha = it.fecha
            val categoriasDelTipo = CategoriasFinanzas.paraTipo(it.tipo)
            val coincidencia = categoriasDelTipo.firstOrNull { c -> c.equals(it.categoria, ignoreCase = true) }
            if (coincidencia != null) {
                categoria = coincidencia
            } else {
                categoria = CategoriasFinanzas.OTROS
                categoriaPersonalizada = it.categoria
            }
        }
        snapshotInicial = snapshot()
    }

    val categorias = CategoriasFinanzas.paraTipo(tipo)
    LaunchedEffect(tipo) {
        if (categoria != null && categoria !in categorias) categoria = null
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = if (viewModel.esEdicion) "Editar movimiento" else "Registrar movimiento",
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = onVerFinanzas, modifier = Modifier.padding(top = 4.dp)) {
            Text("💰 Ver Finanzas y resumen")
        }

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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        Text(text = "Categoría", modifier = Modifier.padding(top = 16.dp))
        FlowRow(modifier = Modifier.padding(top = 8.dp)) {
            categorias.forEach { opcion ->
                FilterChip(
                    selected = categoria == opcion,
                    onClick = { categoria = opcion },
                    label = { Text(opcion) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                )
            }
        }

        if (categoria == CategoriasFinanzas.OTROS) {
            DictationTextField(
                value = categoriaPersonalizada,
                onValueChange = { categoriaPersonalizada = it },
                label = "¿Cuál?",
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        DictationTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = "Descripción (opcional)",
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(text = "Fecha", modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(
                selected = true,
                onClick = { mostrarSelectorFecha = true },
                label = { Text(DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fecha))) },
            )
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            Button(
                onClick = {
                    val categoriaFinal = if (categoria == CategoriasFinanzas.OTROS) {
                        CategoriasFinanzas.encajarEnConocida(categoriaPersonalizada, categorias)
                            .ifBlank { CategoriasFinanzas.OTROS }
                    } else {
                        categoria ?: CategoriasFinanzas.OTROS
                    }
                    viewModel.guardar(tipo, montoTexto.toDoubleOrNull() ?: 0.0, categoriaFinal, descripcion.ifBlank { null }, fecha)
                },
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Text("Guardar")
            }
            if (viewModel.esEdicion) {
                OutlinedButton(onClick = { mostrarConfirmacion = true }) {
                    Text("Eliminar")
                }
            }
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
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina este movimiento para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminar() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }
}
