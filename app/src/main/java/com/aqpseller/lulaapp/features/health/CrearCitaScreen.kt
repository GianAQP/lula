package com.aqpseller.lulaapp.features.health

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.DescartarCambiosAlSalir
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.HoraSelector
import com.aqpseller.lulaapp.core.ui.NivelRecordatorioSelector
import com.aqpseller.lulaapp.core.ui.SelectorRow
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecordatorioCita

private fun etiquetaAnticipacion(anticipacion: AnticipacionRecordatorio): String = when (anticipacion) {
    AnticipacionRecordatorio.MISMO_DIA -> "El mismo día"
    AnticipacionRecordatorio.UN_DIA_ANTES -> "Un día antes"
    AnticipacionRecordatorio.UNA_SEMANA_ANTES -> "Una semana antes"
}

private fun resumenRecordatorios(recordatorios: Map<AnticipacionRecordatorio, String>): String = when (recordatorios.size) {
    0 -> "Sin recordatorios"
    1 -> {
        val (anticipacion, hora) = recordatorios.entries.first()
        "${etiquetaAnticipacion(anticipacion)} a las $hora"
    }
    else -> "${recordatorios.size} recordatorios"
}

private val DIAS_LABORABLES = setOf(1, 2, 3, 4, 5)

private fun resumenRepeticion(esCurso: Boolean, diasSemana: Set<Int>, horaSesion: String?, cantidadTexto: String): String {
    if (!esCurso) return "Una sola vez"
    if (diasSemana.isEmpty()) return "Curso — elige los días"
    val dias = diasSemana.sorted().joinToString(", ") { DateTimeUtils.nombreDiaIso(it).replaceFirstChar(Char::uppercase) }
    val hora = horaSesion ?: "sin hora"
    val cantidad = cantidadTexto.toIntOrNull()
    return "$dias · $hora · " + (if (cantidad != null) "$cantidad sesiones" else "sin cantidad fija")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCitaScreen(
    onGuardado: () -> Unit,
    onVerSalud: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearCitaViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var lugar by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(DateTimeUtils.inicioDeHoyEpochMillis()) }
    var hora by remember { mutableStateOf<String?>(null) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    // Mapa anticipación → hora propia de ese recordatorio — puede haber varios a la vez (ej.
    // "un día antes a las 20:00" y "el mismo día a las 7:00"), cada uno a su propia hora, no
    // necesariamente la hora de la cita.
    var recordatorios by remember { mutableStateOf(mapOf(AnticipacionRecordatorio.UN_DIA_ANTES to "20:00")) }
    var nivelRecordatorio by remember { mutableStateOf(NivelRecordatorio.SONIDO) }
    var mostrarSelectorRecordatorios by remember { mutableStateOf(false) }
    // "Curso" = varias sesiones (ej. radioterapia, masajes) en vez de una cita puntual — ver
    // `Plan/08-decisiones-tecnicas.md`. Cuando está activo, "Fecha"/"Hora" de arriba dejan de
    // usarse y se reemplazan por fecha de inicio + hora de sesión, acá abajo.
    var esCurso by remember { mutableStateOf(false) }
    var diasSemana by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var horaSesion by remember { mutableStateOf<String?>(null) }
    var fechaInicioCurso by remember { mutableStateOf(DateTimeUtils.inicioDeHoyEpochMillis()) }
    var mostrarSelectorFechaInicioCurso by remember { mutableStateOf(false) }
    var cantidadSesionesTexto by remember { mutableStateOf("") }
    var mostrarSelectorRepeticion by remember { mutableStateOf(false) }

    fun snapshot() = listOf(
        nombre, lugar, motivo, fecha, hora, recordatorios, nivelRecordatorio,
        esCurso, diasSemana, horaSesion, fechaInicioCurso, cantidadSesionesTexto,
    )
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
            lugar = inicial.lugar.orEmpty()
            motivo = inicial.motivo.orEmpty()
            fecha = DateTimeUtils.inicioDeDiaEpochMillis(inicial.fechaHora)
            hora = DateTimeUtils.horaHHmm(inicial.fechaHora)
            recordatorios = inicial.recordatorios.associate { it.anticipacion to it.hora }
            nivelRecordatorio = inicial.nivelRecordatorio
            esCurso = inicial.esCurso
            diasSemana = inicial.diasSemana
            horaSesion = inicial.horaSesion
            fechaInicioCurso = inicial.fechaInicioCurso ?: DateTimeUtils.inicioDeHoyEpochMillis()
            cantidadSesionesTexto = inicial.cantidadSesionesTotal?.toString() ?: ""
        }
        snapshotInicial = snapshot()
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = if (viewModel.esEdicion) "Editar cita" else "Nueva cita", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onVerSalud, modifier = Modifier.padding(top = 4.dp)) {
            Text("💊 Ver mi salud")
        }

        DictationTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre (ej. Control con el dentista)",
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = lugar,
            onValueChange = { lugar = it },
            label = "Lugar (opcional)",
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = motivo,
            onValueChange = { motivo = it },
            label = "Motivo (opcional)",
            modifier = Modifier.padding(top = 16.dp),
        )

        if (!esCurso) {
            Text(text = "Fecha", modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = { mostrarSelectorFecha = true },
                    label = { Text(DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fecha))) },
                )
            }

            Text(text = "Hora", modifier = Modifier.padding(top = 16.dp))
            HoraSelector(
                hora = hora,
                onHoraSeleccionada = { hora = it },
                etiqueta = "¿A qué hora es la cita?",
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Column(modifier = Modifier.padding(top = 16.dp)) {
            HorizontalDivider()
            SelectorRow(
                etiqueta = "🔁 Repetición",
                valor = resumenRepeticion(esCurso, diasSemana, horaSesion, cantidadSesionesTexto),
                onClick = { mostrarSelectorRepeticion = true },
            )
            HorizontalDivider()
            SelectorRow(
                etiqueta = "🔔 Recordatorios",
                valor = resumenRecordatorios(recordatorios),
                onClick = { mostrarSelectorRecordatorios = true },
            )
            HorizontalDivider()
        }

        Button(
            onClick = {
                val horaFinal = hora ?: "09:00"
                viewModel.guardar(
                    nombre,
                    lugar.ifBlank { null },
                    motivo.ifBlank { null },
                    if (esCurso) DateTimeUtils.combinarFechaYHora(fechaInicioCurso, horaSesion ?: "09:00") else DateTimeUtils.combinarFechaYHora(fecha, horaFinal),
                    recordatorios.map { (anticipacion, horaRecordatorio) -> RecordatorioCita(anticipacion, horaRecordatorio) },
                    nivelRecordatorio,
                    esCurso,
                    diasSemana,
                    horaSesion,
                    if (esCurso) fechaInicioCurso else null,
                    if (esCurso) cantidadSesionesTexto.toIntOrNull() else null,
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    if (mostrarSelectorRecordatorios) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorRecordatorios = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(text = "Recordarme", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Puedes activar más de uno, cada uno con su propia hora — no tienen que ser a la hora de la cita.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
                AnticipacionRecordatorio.entries.forEach { opcion ->
                    val horaDeEsteRecordatorio = recordatorios[opcion]
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = horaDeEsteRecordatorio != null,
                            onClick = {
                                recordatorios = if (horaDeEsteRecordatorio != null) {
                                    recordatorios - opcion
                                } else {
                                    recordatorios + (opcion to "09:00")
                                }
                            },
                            label = { Text(etiquetaAnticipacion(opcion)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        if (horaDeEsteRecordatorio != null) {
                            HoraSelector(
                                hora = horaDeEsteRecordatorio,
                                onHoraSeleccionada = { nuevaHora -> recordatorios = recordatorios + (opcion to nuevaHora) },
                                etiqueta = "¿A qué hora te recuerdo?",
                            )
                        }
                    }
                }

                Text(text = "¿Qué tan insistente?", modifier = Modifier.padding(top = 16.dp))
                NivelRecordatorioSelector(
                    nivelSeleccionado = nivelRecordatorio,
                    onNivelSeleccionado = { nivelRecordatorio = it },
                    modifier = Modifier.padding(top = 8.dp),
                )

                Button(
                    onClick = { mostrarSelectorRecordatorios = false },
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

    if (mostrarSelectorRepeticion) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorRepeticion = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            // Sheet largo con un campo numérico cerca del final — sin scroll + imePadding, el
            // teclado lo tapaba entero al enfocar "¿Cuántas sesiones?" (reportado por el usuario).
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(text = "¿Se repite?", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    FilterChip(
                        selected = !esCurso,
                        onClick = { esCurso = false },
                        label = { Text("Una sola vez") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = esCurso,
                        onClick = { esCurso = true },
                        label = { Text("Curso (varias sesiones)") },
                    )
                }

                if (esCurso) {
                    Text(
                        text = "Para citas que se repiten en un patrón, como radioterapia o sesiones de masaje.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Text(text = "¿Qué días?", modifier = Modifier.padding(top = 16.dp))
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        FilterChip(
                            selected = diasSemana == DIAS_LABORABLES,
                            onClick = { diasSemana = DIAS_LABORABLES },
                            label = { Text("Días laborables") },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        (1..7).forEach { dia ->
                            FilterChip(
                                selected = dia in diasSemana,
                                onClick = { diasSemana = if (dia in diasSemana) diasSemana - dia else diasSemana + dia },
                                label = { Text(DateTimeUtils.nombreDiaIso(dia).take(1).uppercase()) },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }

                    Text(text = "Fecha de inicio", modifier = Modifier.padding(top = 16.dp))
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        FilterChip(
                            selected = true,
                            onClick = { mostrarSelectorFechaInicioCurso = true },
                            label = { Text(DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaInicioCurso))) },
                        )
                    }

                    Text(text = "Hora de cada sesión", modifier = Modifier.padding(top = 16.dp))
                    HoraSelector(
                        hora = horaSesion,
                        onHoraSeleccionada = { horaSesion = it },
                        etiqueta = "¿A qué hora es cada sesión?",
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Text(text = "¿Cuántas sesiones en total?", modifier = Modifier.padding(top = 16.dp))
                    OutlinedTextField(
                        value = cantidadSesionesTexto,
                        onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) cantidadSesionesTexto = nuevo },
                        label = { Text("Ej. 20 — vacío si no tiene cantidad fija") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }

                Button(
                    onClick = { mostrarSelectorRepeticion = false },
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                ) {
                    Text("Listo")
                }
            }
        }
    }

    if (mostrarSelectorFechaInicioCurso) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(fechaInicioCurso),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFechaInicioCurso = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis ->
                        fechaInicioCurso = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)
                    }
                    mostrarSelectorFechaInicioCurso = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFechaInicioCurso = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}
