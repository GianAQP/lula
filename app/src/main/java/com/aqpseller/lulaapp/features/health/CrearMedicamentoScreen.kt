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
import com.aqpseller.lulaapp.core.utils.calcularFechaFinPorCantidadDosis
import com.aqpseller.lulaapp.core.utils.calcularHorariosPorComida
import com.aqpseller.lulaapp.core.utils.calcularHorariosPorIntervalo
import com.aqpseller.lulaapp.domain.model.Comida
import com.aqpseller.lulaapp.domain.model.ComidaRelacionada
import com.aqpseller.lulaapp.domain.model.ModoFrecuenciaMedicamento
import com.aqpseller.lulaapp.domain.model.MomentoRelativoComida
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio

private fun etiquetaComida(comida: Comida): String = when (comida) {
    Comida.DESAYUNO -> "🌅 Desayuno"
    Comida.ALMUERZO -> "🍲 Almuerzo"
    Comida.CENA -> "🌙 Cena"
}

private fun etiquetaNivel(nivel: NivelRecordatorio): String = when (nivel) {
    NivelRecordatorio.SILENCIOSO -> "🔇 Silencioso"
    NivelRecordatorio.SONIDO -> "🔔 Sonido"
    NivelRecordatorio.ALARMA -> "⏰ Alarma"
}

/** Solo de UI — no es parte del dominio, decide qué preguntar (no se guardan ambos a la vez). */
private enum class ModoFinMedicamento { SIN_FIN, FECHA, DOSIS }

/** Qué selector modal está abierto — null significa ninguno (formulario compacto de base). */
private enum class SelectorAbierto { FRECUENCIA, TERMINA, RECORDATORIO }

/** Mismo cálculo que usan los casos de uso, para mostrar la fecha de fin en vivo mientras se escribe. */
private fun horariosSegunModo(
    modoFrecuencia: ModoFrecuenciaMedicamento,
    horaPrimeraDosis: String?,
    intervaloHoras: Int?,
    comidasSeleccionadas: List<ComidaRelacionada>,
    horariosComida: HorariosComida,
): List<String> = when (modoFrecuencia) {
    ModoFrecuenciaMedicamento.INTERVALO_HORAS ->
        if (horaPrimeraDosis != null && intervaloHoras != null) {
            calcularHorariosPorIntervalo(horaPrimeraDosis, intervaloHoras)
        } else {
            emptyList()
        }
    ModoFrecuenciaMedicamento.RELACION_COMIDA ->
        calcularHorariosPorComida(comidasSeleccionadas, horariosComida.desayuno, horariosComida.almuerzo, horariosComida.cena)
}

private fun resumenFrecuencia(modoFrecuencia: ModoFrecuenciaMedicamento, horaPrimeraDosis: String?, intervaloTexto: String, comidasSeleccionadas: List<ComidaRelacionada>): String =
    when (modoFrecuencia) {
        ModoFrecuenciaMedicamento.INTERVALO_HORAS ->
            if (horaPrimeraDosis != null) "Cada $intervaloTexto h desde $horaPrimeraDosis" else "Sin configurar"
        ModoFrecuenciaMedicamento.RELACION_COMIDA ->
            if (comidasSeleccionadas.isEmpty()) "Sin comidas elegidas" else "Según ${comidasSeleccionadas.size} comida(s)"
    }

private fun resumenTermina(modoFin: ModoFinMedicamento, fechaFin: Long?, dosisTotalTexto: String): String = when (modoFin) {
    ModoFinMedicamento.SIN_FIN -> "Sin fecha de fin"
    ModoFinMedicamento.FECHA -> fechaFin?.let { DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(it)) } ?: "Elegir fecha"
    ModoFinMedicamento.DOSIS -> if (dosisTotalTexto.isBlank()) "Elegir cantidad" else "$dosisTotalTexto dosis en total"
}

private fun resumenRecordatorio(nivel: NivelRecordatorio, persistente: Boolean, intervaloTexto: String): String {
    val base = etiquetaNivel(nivel)
    return if (persistente && intervaloTexto.isNotBlank()) "$base · insiste cada $intervaloTexto min" else base
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearMedicamentoScreen(
    onGuardado: () -> Unit,
    onVerSalud: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearMedicamentoViewModel = hiltViewModel(),
) {
    var nombreMedicamento by remember { mutableStateOf("") }
    var dosis by remember { mutableStateOf("") }
    var modoFrecuencia by remember { mutableStateOf(ModoFrecuenciaMedicamento.INTERVALO_HORAS) }
    var horaPrimeraDosis by remember { mutableStateOf<String?>(null) }
    var intervaloTexto by remember { mutableStateOf("8") }
    var comidasSeleccionadas by remember { mutableStateOf<List<ComidaRelacionada>>(emptyList()) }
    var fechaFin by remember { mutableStateOf<Long?>(null) }
    var mostrarSelectorFechaFin by remember { mutableStateOf(false) }
    var modoFin by remember { mutableStateOf(ModoFinMedicamento.SIN_FIN) }
    var dosisTotalTexto by remember { mutableStateOf("") }
    var nivelRecordatorio by remember { mutableStateOf(NivelRecordatorio.SONIDO) }
    var recordatorioPersistente by remember { mutableStateOf(false) }
    var intervaloPersistenciaTexto by remember { mutableStateOf("15") }
    var selectorAbierto by remember { mutableStateOf<SelectorAbierto?>(null) }

    fun snapshot() = listOf(
        nombreMedicamento, dosis, modoFrecuencia, horaPrimeraDosis, intervaloTexto, comidasSeleccionadas,
        fechaFin, modoFin, dosisTotalTexto, nivelRecordatorio, recordatorioPersistente, intervaloPersistenciaTexto,
    )
    var snapshotInicial by remember { mutableStateOf(snapshot()) }

    val guardado by viewModel.guardado.collectAsState()
    val formInicial by viewModel.formInicial.collectAsState()
    val horariosComida by viewModel.horariosComida.collectAsState()
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
            nombreMedicamento = inicial.nombreMedicamento
            dosis = inicial.dosis
            modoFrecuencia = inicial.modoFrecuencia
            horaPrimeraDosis = inicial.horaPrimeraDosis
            intervaloTexto = inicial.intervaloHoras?.toString() ?: "8"
            comidasSeleccionadas = inicial.comidasRelacionadas
            fechaFin = inicial.fechaFin
            modoFin = when {
                inicial.cantidadDosisTotal != null -> ModoFinMedicamento.DOSIS
                inicial.fechaFin != null -> ModoFinMedicamento.FECHA
                else -> ModoFinMedicamento.SIN_FIN
            }
            dosisTotalTexto = inicial.cantidadDosisTotal?.toString() ?: ""
            nivelRecordatorio = inicial.nivelRecordatorio
            recordatorioPersistente = inicial.recordatorioPersistente
            intervaloPersistenciaTexto = inicial.intervaloPersistenciaMin?.toString() ?: "15"
        }
        snapshotInicial = snapshot()
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = if (viewModel.esEdicion) "Editar medicamento" else "Nuevo medicamento", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onVerSalud, modifier = Modifier.padding(top = 4.dp)) {
            Text("💊 Ver mi salud")
        }

        Text(
            text = "Lula solo te recuerda lo que indicaste acá — nunca sugiere cambiar la dosis, el horario ni suspender el medicamento.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )

        DictationTextField(
            value = nombreMedicamento,
            onValueChange = { nombreMedicamento = it },
            label = "Nombre del medicamento",
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = dosis,
            onValueChange = { dosis = it },
            label = "Dosis (ej. 1 pastilla, 5 ml)",
            modifier = Modifier.padding(top = 16.dp),
        )

        Column(modifier = Modifier.padding(top = 8.dp)) {
            HorizontalDivider()
            SelectorRow(
                etiqueta = "⏰ Frecuencia",
                valor = resumenFrecuencia(modoFrecuencia, horaPrimeraDosis, intervaloTexto, comidasSeleccionadas),
                onClick = { selectorAbierto = SelectorAbierto.FRECUENCIA },
            )
            HorizontalDivider()
            SelectorRow(
                etiqueta = "🏁 Termina",
                valor = resumenTermina(modoFin, fechaFin, dosisTotalTexto),
                onClick = { selectorAbierto = SelectorAbierto.TERMINA },
            )
            HorizontalDivider()
            SelectorRow(
                etiqueta = "🔔 Recordatorio",
                valor = resumenRecordatorio(nivelRecordatorio, recordatorioPersistente, intervaloPersistenciaTexto),
                onClick = { selectorAbierto = SelectorAbierto.RECORDATORIO },
            )
            HorizontalDivider()
        }

        Button(
            onClick = {
                viewModel.guardar(
                    nombreMedicamento,
                    dosis,
                    modoFrecuencia,
                    intervaloTexto.toIntOrNull(),
                    horaPrimeraDosis,
                    comidasSeleccionadas,
                    if (modoFin == ModoFinMedicamento.FECHA) fechaFin else null,
                    if (modoFin == ModoFinMedicamento.DOSIS) dosisTotalTexto.toIntOrNull() else null,
                    nivelRecordatorio,
                    recordatorioPersistente,
                    if (recordatorioPersistente) intervaloPersistenciaTexto.toIntOrNull() else null,
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    when (selectorAbierto) {
        SelectorAbierto.FRECUENCIA -> {
            SelectorFrecuenciaSheet(
                modoFrecuencia = modoFrecuencia,
                onModoFrecuenciaCambiado = { modoFrecuencia = it },
                horaPrimeraDosis = horaPrimeraDosis,
                onHoraPrimeraDosisCambiada = { horaPrimeraDosis = it },
                intervaloTexto = intervaloTexto,
                onIntervaloCambiado = { intervaloTexto = it },
                comidasSeleccionadas = comidasSeleccionadas,
                onComidasCambiadas = { comidasSeleccionadas = it },
                horariosComida = horariosComida,
                onHorarioComidaCambiado = viewModel::actualizarHorarioComida,
                onCerrar = { selectorAbierto = null },
            )
        }
        SelectorAbierto.TERMINA -> {
            SelectorTerminaSheet(
                modoFin = modoFin,
                onModoFinCambiado = { modoFin = it },
                fechaFin = fechaFin,
                onElegirFecha = { mostrarSelectorFechaFin = true },
                dosisTotalTexto = dosisTotalTexto,
                onDosisTotalCambiada = { dosisTotalTexto = it },
                fechaFinCalculada = if (modoFin == ModoFinMedicamento.DOSIS) {
                    val horariosPreview = horariosSegunModo(modoFrecuencia, horaPrimeraDosis, intervaloTexto.toIntOrNull(), comidasSeleccionadas, horariosComida)
                    val cantidadPreview = dosisTotalTexto.toIntOrNull()
                    if (cantidadPreview != null && horariosPreview.isNotEmpty()) {
                        calcularFechaFinPorCantidadDosis(DateTimeUtils.inicioDeHoyEpochMillis(), horariosPreview, cantidadPreview)
                    } else {
                        null
                    }
                } else {
                    null
                },
                onCerrar = { selectorAbierto = null },
            )
        }
        SelectorAbierto.RECORDATORIO -> {
            SelectorRecordatorioSheet(
                nivelRecordatorio = nivelRecordatorio,
                onNivelCambiado = { nivelRecordatorio = it },
                recordatorioPersistente = recordatorioPersistente,
                onRecordatorioPersistenteCambiado = { recordatorioPersistente = it },
                intervaloPersistenciaTexto = intervaloPersistenciaTexto,
                onIntervaloPersistenciaCambiado = { intervaloPersistenciaTexto = it },
                onCerrar = { selectorAbierto = null },
            )
        }
        null -> Unit
    }

    if (mostrarSelectorFechaFin) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(fechaFin ?: DateTimeUtils.inicioDeHoyEpochMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFechaFin = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis ->
                        fechaFin = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)
                    }
                    mostrarSelectorFechaFin = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFechaFin = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFrecuenciaSheet(
    modoFrecuencia: ModoFrecuenciaMedicamento,
    onModoFrecuenciaCambiado: (ModoFrecuenciaMedicamento) -> Unit,
    horaPrimeraDosis: String?,
    onHoraPrimeraDosisCambiada: (String) -> Unit,
    intervaloTexto: String,
    onIntervaloCambiado: (String) -> Unit,
    comidasSeleccionadas: List<ComidaRelacionada>,
    onComidasCambiadas: (List<ComidaRelacionada>) -> Unit,
    horariosComida: HorariosComida,
    onHorarioComidaCambiado: (Comida, String) -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(text = "¿Cada cuánto?", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(top = 12.dp)) {
                FilterChip(
                    selected = modoFrecuencia == ModoFrecuenciaMedicamento.INTERVALO_HORAS,
                    onClick = { onModoFrecuenciaCambiado(ModoFrecuenciaMedicamento.INTERVALO_HORAS) },
                    label = { Text("Cada cierto número de horas") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = modoFrecuencia == ModoFrecuenciaMedicamento.RELACION_COMIDA,
                    onClick = { onModoFrecuenciaCambiado(ModoFrecuenciaMedicamento.RELACION_COMIDA) },
                    label = { Text("Según las comidas") },
                )
            }

            if (modoFrecuencia == ModoFrecuenciaMedicamento.INTERVALO_HORAS) {
                Text(text = "Primera dosis", modifier = Modifier.padding(top = 16.dp))
                HoraSelector(
                    hora = horaPrimeraDosis,
                    onHoraSeleccionada = onHoraPrimeraDosisCambiada,
                    etiqueta = "¿A qué hora es la primera dosis?",
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = intervaloTexto,
                    onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) onIntervaloCambiado(nuevo) },
                    label = { Text("Cada cuántas horas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            } else {
                Text(
                    text = "Elige con qué comidas se relaciona cada toma.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Comida.entries.forEach { comida ->
                    val relacion = comidasSeleccionadas.find { it.comida == comida }
                    FilterChip(
                        selected = relacion != null,
                        onClick = {
                            onComidasCambiadas(
                                if (relacion != null) {
                                    comidasSeleccionadas.filterNot { it.comida == comida }
                                } else {
                                    comidasSeleccionadas + ComidaRelacionada(comida, MomentoRelativoComida.DESPUES)
                                },
                            )
                        },
                        label = { Text(etiquetaComida(comida)) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (relacion != null) {
                        Row(modifier = Modifier.padding(top = 4.dp, start = 16.dp)) {
                            FilterChip(
                                selected = relacion.momento == MomentoRelativoComida.ANTES,
                                onClick = {
                                    onComidasCambiadas(
                                        comidasSeleccionadas.map {
                                            if (it.comida == comida) it.copy(momento = MomentoRelativoComida.ANTES) else it
                                        },
                                    )
                                },
                                label = { Text("Antes") },
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            FilterChip(
                                selected = relacion.momento == MomentoRelativoComida.DESPUES,
                                onClick = {
                                    onComidasCambiadas(
                                        comidasSeleccionadas.map {
                                            if (it.comida == comida) it.copy(momento = MomentoRelativoComida.DESPUES) else it
                                        },
                                    )
                                },
                                label = { Text("Después") },
                            )
                        }
                        val horaGuardada = when (comida) {
                            Comida.DESAYUNO -> horariosComida.desayuno
                            Comida.ALMUERZO -> horariosComida.almuerzo
                            Comida.CENA -> horariosComida.cena
                        }
                        if (horaGuardada == null) {
                            Text(
                                text = "¿A qué hora sueles ${if (comida == Comida.DESAYUNO) "desayunar" else if (comida == Comida.ALMUERZO) "almorzar" else "cenar"}?",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp, start = 16.dp),
                            )
                            HoraSelector(
                                hora = null,
                                onHoraSeleccionada = { onHorarioComidaCambiado(comida, it) },
                                etiqueta = "Elegir hora",
                                modifier = Modifier.padding(top = 4.dp, start = 16.dp),
                            )
                        } else {
                            Text(
                                text = "Guardado: $horaGuardada",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp, start = 16.dp),
                            )
                        }
                    }
                }
            }

            Button(onClick = onCerrar, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                Text("Listo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTerminaSheet(
    modoFin: ModoFinMedicamento,
    onModoFinCambiado: (ModoFinMedicamento) -> Unit,
    fechaFin: Long?,
    onElegirFecha: () -> Unit,
    dosisTotalTexto: String,
    onDosisTotalCambiada: (String) -> Unit,
    fechaFinCalculada: Long?,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(text = "¿Cuándo termina? (opcional)", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(top = 12.dp)) {
                FilterChip(
                    selected = modoFin == ModoFinMedicamento.SIN_FIN,
                    onClick = { onModoFinCambiado(ModoFinMedicamento.SIN_FIN) },
                    label = { Text("Sin fecha de fin") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = modoFin == ModoFinMedicamento.FECHA,
                    onClick = {
                        onModoFinCambiado(ModoFinMedicamento.FECHA)
                        onElegirFecha()
                    },
                    label = {
                        Text(
                            if (modoFin == ModoFinMedicamento.FECHA && fechaFin != null) {
                                DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaFin))
                            } else {
                                "📅 Elegir fecha"
                            },
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = modoFin == ModoFinMedicamento.DOSIS,
                    onClick = { onModoFinCambiado(ModoFinMedicamento.DOSIS) },
                    label = { Text("🔢 Cantidad de dosis") },
                )
            }

            if (modoFin == ModoFinMedicamento.DOSIS) {
                OutlinedTextField(
                    value = dosisTotalTexto,
                    onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) onDosisTotalCambiada(nuevo) },
                    label = { Text("¿Cuántas dosis en total recetó el médico?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    text = if (fechaFinCalculada != null) {
                        "Termina el " + DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaFinCalculada))
                    } else {
                        "Escribe la primera dosis, cada cuántas horas, y la cantidad — acá se muestra sola la fecha de fin."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Button(onClick = onCerrar, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                Text("Listo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRecordatorioSheet(
    nivelRecordatorio: NivelRecordatorio,
    onNivelCambiado: (NivelRecordatorio) -> Unit,
    recordatorioPersistente: Boolean,
    onRecordatorioPersistenteCambiado: (Boolean) -> Unit,
    intervaloPersistenciaTexto: String,
    onIntervaloPersistenciaCambiado: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(text = "¿Qué tan insistente?", style = MaterialTheme.typography.titleMedium)
            NivelRecordatorioSelector(
                nivelSeleccionado = nivelRecordatorio,
                onNivelSeleccionado = onNivelCambiado,
                modifier = Modifier.padding(top = 12.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(top = 20.dp))
            Text(
                text = "🔁 Insistir hasta que lo marques",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Si no marcas la toma, Lula vuelve a recordarte cada cierto tiempo, hasta que la marques o termine el día.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                FilterChip(
                    selected = !recordatorioPersistente,
                    onClick = { onRecordatorioPersistenteCambiado(false) },
                    label = { Text("Solo una vez") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = recordatorioPersistente,
                    onClick = { onRecordatorioPersistenteCambiado(true) },
                    label = { Text("Insistir") },
                )
            }
            if (recordatorioPersistente) {
                OutlinedTextField(
                    value = intervaloPersistenciaTexto,
                    onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) onIntervaloPersistenciaCambiado(nuevo) },
                    label = { Text("Cada cuántos minutos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }

            Button(onClick = onCerrar, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                Text("Listo")
            }
        }
    }
}
