package com.aqpseller.lulaapp.features.tasks

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.aqpseller.lulaapp.core.ui.HoraRecordatorioSelector
import com.aqpseller.lulaapp.core.ui.NivelRecordatorioSelector
import com.aqpseller.lulaapp.core.ui.RecurrenciaTareaSelector
import com.aqpseller.lulaapp.core.ui.SelectorRow
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

private enum class OpcionFecha { SIN_FECHA, HOY, MANANA, PERSONALIZADA }

private fun mananaEpochMillis(): Long = DateTimeUtils.hoy()
    .plus(DatePeriod(days = 1))
    .atStartOfDayIn(TimeZone.currentSystemDefault())
    .toEpochMilliseconds()

private fun fechaLimiteDe(opcionFecha: OpcionFecha, fechaPersonalizada: Long?): Long? = when (opcionFecha) {
    OpcionFecha.SIN_FECHA -> null
    OpcionFecha.HOY -> DateTimeUtils.inicioDeHoyEpochMillis()
    OpcionFecha.MANANA -> mananaEpochMillis()
    OpcionFecha.PERSONALIZADA -> fechaPersonalizada
}

private fun etiquetaNivel(nivel: NivelRecordatorio): String = when (nivel) {
    NivelRecordatorio.SILENCIOSO -> "🔇 Silencioso"
    NivelRecordatorio.SONIDO -> "🔔 Sonido"
    NivelRecordatorio.ALARMA -> "⏰ Alarma"
}

private fun resumenFecha(opcionFecha: OpcionFecha, fechaPersonalizada: Long?): String = when (opcionFecha) {
    OpcionFecha.SIN_FECHA -> "Sin fecha"
    OpcionFecha.HOY -> "Hoy"
    OpcionFecha.MANANA -> "Mañana"
    OpcionFecha.PERSONALIZADA ->
        fechaPersonalizada?.let { DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(it)) } ?: "Elegir fecha"
}

private fun resumenRecordatorio(horaRecordatorio: String?, nivelRecordatorio: NivelRecordatorio): String =
    if (horaRecordatorio == null) "Sin recordatorio" else "$horaRecordatorio · ${etiquetaNivel(nivelRecordatorio)}"

private fun resumenVinculo(actividadVinculadaId: String?, opciones: List<ActividadVinculableUi>): String =
    opciones.firstOrNull { it.id == actividadVinculadaId }?.let { "${it.emoji} ${it.nombre}" } ?: "Sin vincular"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearTareaScreen(
    onGuardado: () -> Unit,
    onVerTareas: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearTareaViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var opcionFecha by remember { mutableStateOf(OpcionFecha.SIN_FECHA) }
    var fechaPersonalizada by remember { mutableStateOf<Long?>(null) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var importante by remember { mutableStateOf(false) }
    var urgente by remember { mutableStateOf(false) }
    var horaRecordatorio by remember { mutableStateOf<String?>(null) }
    var nivelRecordatorio by remember { mutableStateOf(NivelRecordatorio.SONIDO) }
    var recurrencia by remember { mutableStateOf(RecurrenciaTarea.SIN_REPETIR) }
    var actividadVinculadaId by remember { mutableStateOf<String?>(null) }
    var mostrarSelectorFechaLimite by remember { mutableStateOf(false) }
    var mostrarSelectorRecordatorio by remember { mutableStateOf(false) }
    var mostrarSelectorVinculo by remember { mutableStateOf(false) }

    // Snapshot del formulario para saber si hay cambios sin guardar — comparado contra el
    // estado justo después de cargar (edición) o contra los valores en blanco (creación), así
    // que sirve para los dos modos, no solo para "el nombre no está vacío" (ver
    // `Plan/08-decisiones-tecnicas.md`).
    fun snapshot() = listOf(nombre, opcionFecha, fechaPersonalizada, importante, urgente, horaRecordatorio, nivelRecordatorio, recurrencia, actividadVinculadaId)
    var snapshotInicial by remember { mutableStateOf(snapshot()) }

    val context = LocalContext.current
    val guardado by viewModel.guardado.collectAsState()
    val formInicial by viewModel.formInicial.collectAsState()
    val actividadesVinculables by viewModel.actividadesVinculables.collectAsState()
    val mensajeError by viewModel.mensajeError.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.errorMostrado()
        }
    }
    DescartarCambiosAlSalir(
        hayContenidoSinGuardar = snapshotInicial != snapshot() && !guardado,
        onDescartar = onSalirSinGuardar,
    )
    LaunchedEffect(formInicial) {
        formInicial?.let { inicial ->
            nombre = inicial.nombre
            importante = inicial.importante
            urgente = inicial.urgente
            horaRecordatorio = inicial.horaRecordatorio
            nivelRecordatorio = inicial.nivelRecordatorio
            recurrencia = inicial.recurrencia
            actividadVinculadaId = inicial.actividadVinculadaId
            opcionFecha = when (inicial.fechaLimite) {
                null -> OpcionFecha.SIN_FECHA
                DateTimeUtils.inicioDeHoyEpochMillis() -> OpcionFecha.HOY
                mananaEpochMillis() -> OpcionFecha.MANANA
                else -> {
                    fechaPersonalizada = inicial.fechaLimite
                    OpcionFecha.PERSONALIZADA
                }
            }
        }
        snapshotInicial = snapshot()
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = if (viewModel.esEdicion) "Editar tarea" else "Nueva tarea", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onVerTareas, modifier = Modifier.padding(top = 4.dp)) {
            Text("📝 Ver todas mis tareas")
        }

        DictationTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre",
            modifier = Modifier.padding(top = 16.dp),
        )

        Row(modifier = Modifier.padding(top = 16.dp)) {
            FilterChip(
                selected = importante,
                onClick = { importante = !importante },
                label = { Text("Importante") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = urgente,
                onClick = { urgente = !urgente },
                label = { Text("Urgente") },
            )
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            HorizontalDivider()
            SelectorRow(
                etiqueta = "📅 Fecha límite",
                valor = resumenFecha(opcionFecha, fechaPersonalizada),
                onClick = { mostrarSelectorFechaLimite = true },
            )
            if (opcionFecha != OpcionFecha.SIN_FECHA) {
                HorizontalDivider()
                SelectorRow(
                    etiqueta = "🔔 Recordatorio",
                    valor = resumenRecordatorio(horaRecordatorio, nivelRecordatorio),
                    onClick = { mostrarSelectorRecordatorio = true },
                )
            }
            // Antes esto vivía siempre desplegado (título + explicación + un chip por cada
            // medicamento/cita existente) — se veía muy cargado apenas se abría el formulario,
            // sobre todo con varios medicamentos. Ahora es una fila colapsada más, mismo patrón
            // que Fecha límite/Recordatorio, y las opciones solo aparecen al tocarla. A pedido
            // del usuario. Ver `Plan/08-decisiones-tecnicas.md`.
            if (actividadesVinculables.isNotEmpty()) {
                HorizontalDivider()
                SelectorRow(
                    etiqueta = "🔗 ¿Acompaña a un medicamento o cita?",
                    valor = resumenVinculo(actividadVinculadaId, actividadesVinculables),
                    onClick = { mostrarSelectorVinculo = true },
                )
            }
            HorizontalDivider()
        }

        Button(
            onClick = {
                viewModel.guardar(
                    nombre,
                    fechaLimiteDe(opcionFecha, fechaPersonalizada),
                    importante,
                    urgente,
                    horaRecordatorio,
                    nivelRecordatorio,
                    recurrencia,
                    actividadVinculadaId,
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    if (mostrarSelectorFechaLimite) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorFechaLimite = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(text = "Fecha límite", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    FilterChip(
                        selected = opcionFecha == OpcionFecha.SIN_FECHA,
                        onClick = {
                            opcionFecha = OpcionFecha.SIN_FECHA
                            horaRecordatorio = null
                            recurrencia = RecurrenciaTarea.SIN_REPETIR
                        },
                        label = { Text("Sin fecha") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = opcionFecha == OpcionFecha.HOY,
                        onClick = { opcionFecha = OpcionFecha.HOY },
                        label = { Text("Hoy") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = opcionFecha == OpcionFecha.MANANA,
                        onClick = { opcionFecha = OpcionFecha.MANANA },
                        label = { Text("Mañana") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = opcionFecha == OpcionFecha.PERSONALIZADA,
                        onClick = { mostrarSelectorFecha = true },
                        label = {
                            Text(
                                if (opcionFecha == OpcionFecha.PERSONALIZADA && fechaPersonalizada != null) {
                                    DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaPersonalizada!!))
                                } else {
                                    "📅 Elegir fecha"
                                },
                            )
                        },
                    )
                }

                if (opcionFecha != OpcionFecha.SIN_FECHA) {
                    Text(text = "¿Se repite?", modifier = Modifier.padding(top = 16.dp))
                    Text(
                        text = "Para pagos y trámites periódicos (luz, agua, etc.) — al marcarla hecha, " +
                            "vuelve a aparecer sola en la próxima fecha, como un hábito.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    RecurrenciaTareaSelector(
                        recurrenciaSeleccionada = recurrencia,
                        onRecurrenciaSeleccionada = { recurrencia = it },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Button(
                    onClick = { mostrarSelectorFechaLimite = false },
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                ) {
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
                    Text(text = "¿Qué tan insistente?", modifier = Modifier.padding(top = 16.dp))
                    NivelRecordatorioSelector(
                        nivelSeleccionado = nivelRecordatorio,
                        onNivelSeleccionado = { nivelRecordatorio = it },
                        modifier = Modifier.padding(top = 8.dp),
                    )
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

    if (mostrarSelectorVinculo) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSelectorVinculo = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(text = "¿Acompaña a un medicamento o cita?", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Para el caso de cuidar a alguien por un tiempo — cuando ese medicamento o " +
                        "cita termine, esta tarea se marca como completada sola.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FlowRow(modifier = Modifier.padding(top = 12.dp)) {
                    FilterChip(
                        selected = actividadVinculadaId == null,
                        onClick = { actividadVinculadaId = null },
                        label = { Text("Sin vincular") },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                    )
                    actividadesVinculables.forEach { opcion ->
                        FilterChip(
                            selected = actividadVinculadaId == opcion.id,
                            onClick = { actividadVinculadaId = opcion.id },
                            label = { Text("${opcion.emoji} ${opcion.nombre}") },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                        )
                    }
                }
                Button(
                    onClick = { mostrarSelectorVinculo = false },
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                ) {
                    Text("Listo")
                }
            }
        }
    }

    if (mostrarSelectorFecha) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(
                fechaPersonalizada ?: DateTimeUtils.inicioDeHoyEpochMillis(),
            ),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis ->
                        fechaPersonalizada = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)
                        opcionFecha = OpcionFecha.PERSONALIZADA
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
