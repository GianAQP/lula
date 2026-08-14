package com.aqpseller.lulaapp.features.habits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.aqpseller.lulaapp.core.ui.DescartarCambiosAlSalir
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.HoraRecordatorioSelector
import com.aqpseller.lulaapp.core.ui.NivelRecordatorioSelector
import com.aqpseller.lulaapp.core.ui.SelectorRow
import com.aqpseller.lulaapp.core.ui.etiquetaMomentoDelDia
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio

private fun etiquetaNivel(nivel: NivelRecordatorio): String = when (nivel) {
    NivelRecordatorio.SILENCIOSO -> "🔇 Silencioso"
    NivelRecordatorio.SONIDO -> "🔔 Sonido"
    NivelRecordatorio.ALARMA -> "⏰ Alarma"
}

private fun resumenDuracion(duracionTexto: String, esProgresivo: Boolean, duracionObjetivoTexto: String): String = when {
    duracionTexto.isBlank() -> "Sin configurar"
    esProgresivo && duracionObjetivoTexto.isNotBlank() -> "$duracionTexto min, sube hasta $duracionObjetivoTexto min"
    else -> "$duracionTexto min"
}

private fun resumenRecordatorio(horaRecordatorio: String?, nivelRecordatorio: NivelRecordatorio): String =
    if (horaRecordatorio == null) "Sin recordatorio" else "$horaRecordatorio · ${etiquetaNivel(nivelRecordatorio)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearHabitoScreen(
    onGuardado: () -> Unit,
    onVerHabitos: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearHabitoViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var momento by remember { mutableStateOf(MomentoDelDia.MANANA) }
    var duracionTexto by remember { mutableStateOf("") }
    var horaRecordatorio by remember { mutableStateOf<String?>(null) }
    var nivelRecordatorio by remember { mutableStateOf(NivelRecordatorio.SONIDO) }
    var esProgresivo by remember { mutableStateOf(false) }
    var duracionObjetivoTexto by remember { mutableStateOf("") }
    var incrementoTexto by remember { mutableStateOf("") }
    var frecuenciaRevisionTexto by remember { mutableStateOf("") }
    var mostrarSelectorDuracion by remember { mutableStateOf(false) }
    var mostrarSelectorRecordatorio by remember { mutableStateOf(false) }

    fun snapshot() = listOf(
        nombre, momento, duracionTexto, horaRecordatorio, nivelRecordatorio,
        esProgresivo, duracionObjetivoTexto, incrementoTexto, frecuenciaRevisionTexto,
    )
    var snapshotInicial by remember { mutableStateOf(snapshot()) }

    val guardado by viewModel.guardado.collectAsState()
    val formInicial by viewModel.formInicial.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    DescartarCambiosAlSalir(
        hayContenidoSinGuardar = snapshotInicial != snapshot() && !guardado,
        onDescartar = onSalirSinGuardar,
    )
    LaunchedEffect(formInicial) {
        formInicial?.let {
            nombre = it.nombre
            momento = it.momentoDelDia
            duracionTexto = it.duracionInicialMin?.toString() ?: ""
            horaRecordatorio = it.horaRecordatorio
            nivelRecordatorio = it.nivelRecordatorio
            duracionObjetivoTexto = it.duracionObjetivoMin?.toString() ?: ""
            incrementoTexto = it.incrementoMin?.toString() ?: ""
            frecuenciaRevisionTexto = it.frecuenciaRevisionDias?.toString() ?: ""
            esProgresivo = it.duracionObjetivoMin != null && it.incrementoMin != null && it.frecuenciaRevisionDias != null
        }
        snapshotInicial = snapshot()
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = if (viewModel.esEdicion) "Editar hábito" else "Nuevo hábito", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onVerHabitos, modifier = Modifier.padding(top = 4.dp)) {
            Text("🌱 Ver mis hábitos")
        }

        DictationTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre",
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(text = "Momento del día", modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            MomentoDelDia.entries.forEach { opcion ->
                FilterChip(
                    selected = momento == opcion,
                    onClick = { momento = opcion },
                    label = { Text(etiquetaMomentoDelDia(opcion)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            HorizontalDivider()
            SelectorRow(
                etiqueta = "⏱️ Duración",
                valor = resumenDuracion(duracionTexto, esProgresivo, duracionObjetivoTexto),
                onClick = { mostrarSelectorDuracion = true },
            )
            HorizontalDivider()
            SelectorRow(
                etiqueta = "🔔 Recordatorio",
                valor = resumenRecordatorio(horaRecordatorio, nivelRecordatorio),
                onClick = { mostrarSelectorRecordatorio = true },
            )
            HorizontalDivider()
        }

        Button(
            onClick = {
                viewModel.guardar(
                    nombre,
                    momento,
                    duracionTexto.toIntOrNull(),
                    horaRecordatorio,
                    nivelRecordatorio,
                    duracionObjetivoTexto.toIntOrNull().takeIf { esProgresivo },
                    incrementoTexto.toIntOrNull().takeIf { esProgresivo },
                    frecuenciaRevisionTexto.toIntOrNull().takeIf { esProgresivo },
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    if (mostrarSelectorDuracion) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorDuracion = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(text = "Duración", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = duracionTexto,
                    onValueChange = { nuevoValor -> if (nuevoValor.all { it.isDigit() }) duracionTexto = nuevoValor },
                    label = { Text("Duración inicial (opcional, min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )

                if (duracionTexto.isNotBlank()) {
                    FilterChip(
                        selected = esProgresivo,
                        onClick = { esProgresivo = !esProgresivo },
                        label = { Text("📈 Aumentar con el tiempo") },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (esProgresivo) {
                        Text(
                            text = "Cada cierto tiempo Lula te va a preguntar si quieres subir la duración — nunca lo decide sola.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        OutlinedTextField(
                            value = duracionObjetivoTexto,
                            onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) duracionObjetivoTexto = nuevo },
                            label = { Text("Objetivo (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = incrementoTexto,
                            onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) incrementoTexto = nuevo },
                            label = { Text("Subir de a cuántos min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = frecuenciaRevisionTexto,
                            onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) frecuenciaRevisionTexto = nuevo },
                            label = { Text("Revisar cada cuántos días") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }

                Button(onClick = { mostrarSelectorDuracion = false }, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                    Text("Listo")
                }
            }
        }
    }

    if (mostrarSelectorRecordatorio) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorRecordatorio = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(text = "Recordarme a las", style = MaterialTheme.typography.titleMedium)
                HoraRecordatorioSelector(
                    horaSeleccionada = horaRecordatorio,
                    onHoraSeleccionada = { horaRecordatorio = it },
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (horaRecordatorio != null) {
                    Text(
                        text = "Se repetirá todos los días a esta hora — no hace falta configurar nada más.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(text = "¿Qué tan insistente?", modifier = Modifier.padding(top = 16.dp))
                    NivelRecordatorioSelector(
                        nivelSeleccionado = nivelRecordatorio,
                        onNivelSeleccionado = { nivelRecordatorio = it },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Button(onClick = { mostrarSelectorRecordatorio = false }, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                    Text("Listo")
                }
            }
        }
    }
}
