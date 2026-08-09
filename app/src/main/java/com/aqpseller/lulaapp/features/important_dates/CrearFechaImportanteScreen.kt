package com.aqpseller.lulaapp.features.important_dates

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.aqpseller.lulaapp.core.ui.DescartarCambiosAlSalir
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.HoraSelector
import com.aqpseller.lulaapp.core.ui.SelectorRow
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.TipoAviso

private fun etiquetaRecurrencia(recurrencia: Recurrencia): String = when (recurrencia) {
    Recurrencia.UNICA -> "Una vez"
    Recurrencia.SEMANAL -> "Cada semana"
    Recurrencia.ANUAL -> "Cada año"
}

private fun etiquetaAnticipacion(anticipacion: AnticipacionRecordatorio): String = when (anticipacion) {
    AnticipacionRecordatorio.MISMO_DIA -> "El mismo día"
    AnticipacionRecordatorio.UN_DIA_ANTES -> "1 día antes"
    AnticipacionRecordatorio.UNA_SEMANA_ANTES -> "1 semana antes"
}

private fun etiquetaTipoAviso(tipoAviso: TipoAviso): String = when (tipoAviso) {
    TipoAviso.ALARMA_SONORA -> "🔔 Alarma con sonido"
    TipoAviso.MENSAJE_SILENCIOSO -> "💬 Solo notificación"
}

private fun resumenRecordatorio(recurrencia: Recurrencia, anticipacion: AnticipacionRecordatorio, horaNotificacion: String): String =
    "${etiquetaRecurrencia(recurrencia)} · ${etiquetaAnticipacion(anticipacion)} · $horaNotificacion"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearFechaImportanteScreen(
    onGuardado: () -> Unit,
    onVerFechasImportantes: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearFechaImportanteViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(DateTimeUtils.inicioDeHoyEpochMillis()) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var recurrencia by remember { mutableStateOf(Recurrencia.ANUAL) }
    var anticipacion by remember { mutableStateOf(AnticipacionRecordatorio.MISMO_DIA) }
    var horaNotificacion by remember { mutableStateOf("09:00") }
    var tipoAviso by remember { mutableStateOf(TipoAviso.MENSAJE_SILENCIOSO) }
    var mostrarSelectorRecordatorio by remember { mutableStateOf(false) }

    fun snapshot() = listOf(nombre, fecha, recurrencia, anticipacion, horaNotificacion, tipoAviso)
    var snapshotInicial by remember { mutableStateOf(snapshot()) }

    val guardado by viewModel.guardado.collectAsState()
    val formInicial by viewModel.formInicial.collectAsState()
    val mensajeError by viewModel.mensajeError.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    DescartarCambiosAlSalir(
        hayContenidoSinGuardar = snapshotInicial != snapshot() && !guardado,
        onDescartar = onSalirSinGuardar,
    )
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.errorMostrado()
        }
    }
    LaunchedEffect(formInicial) {
        formInicial?.let { inicial ->
            nombre = inicial.nombre
            fecha = DateTimeUtils.inicioDeDiaEpochMillis(inicial.fechaBase)
            recurrencia = inicial.recurrencia
            anticipacion = inicial.anticipacion
            horaNotificacion = inicial.horaNotificacion
            tipoAviso = inicial.tipoAviso
        }
        snapshotInicial = snapshot()
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = if (viewModel.esEdicion) "Editar fecha importante" else "Nueva fecha importante",
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = onVerFechasImportantes, modifier = Modifier.padding(top = 4.dp)) {
            Text("🎉 Ver mis fechas importantes")
        }

        DictationTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre (ej. Cumpleaños de mamá)",
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

        Column(modifier = Modifier.padding(top = 16.dp)) {
            HorizontalDivider()
            SelectorRow(
                etiqueta = "🔔 Recordatorio",
                valor = resumenRecordatorio(recurrencia, anticipacion, horaNotificacion),
                onClick = { mostrarSelectorRecordatorio = true },
            )
            HorizontalDivider()
        }

        Button(
            onClick = { viewModel.guardar(nombre, fecha, recurrencia, horaNotificacion, anticipacion, tipoAviso) },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    if (mostrarSelectorRecordatorio) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorRecordatorio = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(text = "Se repite", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Recurrencia.entries.forEach { opcion ->
                        FilterChip(
                            selected = recurrencia == opcion,
                            onClick = { recurrencia = opcion },
                            label = { Text(etiquetaRecurrencia(opcion)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }

                Text(text = "Recordarme", modifier = Modifier.padding(top = 16.dp))
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    AnticipacionRecordatorio.entries.forEach { opcion ->
                        FilterChip(
                            selected = anticipacion == opcion,
                            onClick = { anticipacion = opcion },
                            label = { Text(etiquetaAnticipacion(opcion)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }

                Text(text = "Hora del recordatorio", modifier = Modifier.padding(top = 16.dp))
                HoraSelector(
                    hora = horaNotificacion,
                    onHoraSeleccionada = { horaNotificacion = it },
                    etiqueta = "¿A qué hora te aviso?",
                    modifier = Modifier.padding(top = 8.dp),
                )

                Text(text = "Cómo avisarme", modifier = Modifier.padding(top = 16.dp))
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    TipoAviso.entries.forEach { opcion ->
                        FilterChip(
                            selected = tipoAviso == opcion,
                            onClick = { tipoAviso = opcion },
                            label = { Text(etiquetaTipoAviso(opcion)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }

                Button(
                    onClick = { mostrarSelectorRecordatorio = false },
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                ) {
                    Text("Listo")
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
                    estadoFecha.selectedDateMillis?.let { utcMillis ->
                        fecha = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)
                    }
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
}
