package com.aqpseller.lulaapp.features.goals

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
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.aqpseller.lulaapp.core.ui.DescartarCambiosAlSalir
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta

/** Ayuda de referencia para definir una meta — no se guarda, solo orienta. Ver
 * `Plan/08-decisiones-tecnicas.md`, 2026-08-01 (antes vivía, mal ubicada, dentro de "Mi
 * propósito"). */
private val PREGUNTAS_AYUDA_META = listOf(
    "¿Qué quiero hacer?",
    "¿Qué quiero ser?",
    "¿Qué quiero ver?",
    "¿Qué quiero tener?",
    "¿Adónde quiero ir?",
    "¿Qué es lo que deseo compartir?",
)
private val CONSEJOS_REDACCION_META = listOf(
    "Escribe en tiempo presente — ej. \"Yo gano S/ 5000 al mes\".",
    "Usa oraciones afirmativas — ej. \"Yo superé la adicción al cigarrillo\".",
    "Usa el \"yo\" — ej. \"Yo gano\", \"Yo soy\", \"Yo tengo\".",
)

private fun etiquetaObjetivo(comoSeMide: ComoSeMideMeta) = when (comoSeMide) {
    ComoSeMideMeta.POR_HABITO -> "Objetivo (días de los últimos N)"
    ComoSeMideMeta.POR_MONTO -> "Objetivo (S/)"
    ComoSeMideMeta.POR_NUMERO -> "Objetivo (número)"
    ComoSeMideMeta.MANUAL -> "Objetivo (cantidad a alcanzar)"
}

private fun etiquetaComoSeMide(comoSeMide: ComoSeMideMeta) = when (comoSeMide) {
    ComoSeMideMeta.POR_HABITO -> "Por hábito"
    ComoSeMideMeta.POR_MONTO -> "Por monto"
    ComoSeMideMeta.POR_NUMERO -> "Por número"
    ComoSeMideMeta.MANUAL -> "Manual"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearMetaScreen(
    onGuardado: () -> Unit,
    onVerMetas: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearMetaViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var comoSeMide by remember { mutableStateOf(ComoSeMideMeta.MANUAL) }
    var objetivoTexto by remember { mutableStateOf("") }
    var habitoVinculadoId by remember { mutableStateOf<String?>(null) }
    var areaDeVidaId by remember { mutableStateOf<String?>(null) }
    var fechaLimite by remember { mutableStateOf<Long?>(null) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarAyuda by remember { mutableStateOf(false) }

    fun snapshot() = listOf(nombre, comoSeMide, objetivoTexto, habitoVinculadoId, areaDeVidaId, fechaLimite)
    var snapshotInicial by remember { mutableStateOf(snapshot()) }

    val guardado by viewModel.guardado.collectAsState()
    val habitosDisponibles by viewModel.habitosDisponibles.collectAsState()
    val areasDisponibles by viewModel.areasDisponibles.collectAsState()
    val estadoInicial by viewModel.estadoInicial.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    DescartarCambiosAlSalir(
        hayContenidoSinGuardar = snapshotInicial != snapshot() && !guardado,
        onDescartar = onSalirSinGuardar,
    )
    LaunchedEffect(estadoInicial) {
        estadoInicial?.let {
            nombre = it.nombre
            comoSeMide = it.comoSeMide
            objetivoTexto = it.valorObjetivo.toString()
            habitoVinculadoId = it.actividadesVinculadasIds.firstOrNull()
            areaDeVidaId = it.areaDeVidaId
            fechaLimite = it.fechaLimite
        }
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = if (viewModel.esEdicion) "Editar meta" else "Nueva meta",
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = onVerMetas, modifier = Modifier.padding(top = 4.dp)) {
            Text("🎯 Ver mis metas")
        }

        DictationTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre",
            modifier = Modifier.padding(top = 16.dp),
        )

        TextButton(onClick = { mostrarAyuda = !mostrarAyuda }, modifier = Modifier.padding(top = 4.dp)) {
            Text(if (mostrarAyuda) "💡 Ocultar ideas para definir tu meta" else "💡 ¿No sabes cómo definirla? Ver ideas")
        }
        if (mostrarAyuda) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Preguntas para ayudarte a pensarla", style = MaterialTheme.typography.titleSmall)
                    PREGUNTAS_AYUDA_META.forEach { pregunta ->
                        Text(text = "· $pregunta", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text(
                        text = "Consejos para redactarla",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    CONSEJOS_REDACCION_META.forEach { consejo ->
                        Text(text = "· $consejo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        Text(text = "¿Cómo la vas a medir?", modifier = Modifier.padding(top = 16.dp))
        if (viewModel.esEdicion) {
            Text(
                text = etiquetaComoSeMide(comoSeMide),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "No se puede cambiar después de creada la meta.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            FlowRow(modifier = Modifier.padding(top = 8.dp)) {
                ComoSeMideMeta.entries.forEach { opcion ->
                    FilterChip(
                        selected = comoSeMide == opcion,
                        onClick = { comoSeMide = opcion; habitoVinculadoId = null },
                        label = { Text(etiquetaComoSeMide(opcion)) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }

        if (comoSeMide == ComoSeMideMeta.POR_HABITO) {
            Text(
                text = "Vincula esta meta a un hábito que ya estés siguiendo — el progreso se " +
                    "calculará solo, contando cuántos días lo cumples.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(text = "¿Cuál de tus hábitos ya creados?", modifier = Modifier.padding(top = 8.dp))
            if (habitosDisponibles.isEmpty()) {
                Text(
                    text = "Todavía no tienes hábitos creados. Crea uno primero desde \"+\" → " +
                        "Hábito, o elige otra forma de medir esta meta.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                FlowRow(modifier = Modifier.padding(top = 8.dp)) {
                    habitosDisponibles.forEach { habito ->
                        FilterChip(
                            selected = habitoVinculadoId == habito.id,
                            onClick = { habitoVinculadoId = habito.id },
                            label = { Text(habito.nombre) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        }

        if (comoSeMide == ComoSeMideMeta.POR_MONTO) {
            Text(
                text = "El progreso se calcula solo, sumando lo que registres en Finanzas con la " +
                    "categoría \"Ahorro\" — no hace falta agregarlo a mano acá.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (comoSeMide == ComoSeMideMeta.MANUAL) {
            Text(
                text = "Siempre es un número — vos vas a ir agregando tu avance a mano " +
                    "(ej. 12 de 20 libros, 5 de 10 km). No es para describir la meta con palabras, " +
                    "eso ya lo escribiste arriba en \"Nombre\".",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        OutlinedTextField(
            value = objetivoTexto,
            onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == '.' }) objetivoTexto = nuevo },
            label = { Text(etiquetaObjetivo(comoSeMide)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        if (areasDisponibles.isNotEmpty()) {
            Text(text = "Área de vida (opcional)", modifier = Modifier.padding(top = 16.dp))
            FlowRow(modifier = Modifier.padding(top = 8.dp)) {
                areasDisponibles.forEach { area ->
                    FilterChip(
                        selected = areaDeVidaId == area.id,
                        onClick = { areaDeVidaId = if (areaDeVidaId == area.id) null else area.id },
                        label = { Text(area.nombre) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }

        Text(text = "Fecha límite (opcional)", modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(
                selected = fechaLimite == null,
                onClick = { fechaLimite = null },
                label = { Text("Sin fecha límite") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = fechaLimite != null,
                onClick = { mostrarSelectorFecha = true },
                label = {
                    Text(
                        fechaLimite?.let { DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(it)) }
                            ?: "Elegir fecha",
                    )
                },
            )
        }

        Button(
            onClick = {
                viewModel.guardar(nombre, comoSeMide, objetivoTexto.toDoubleOrNull() ?: 0.0, habitoVinculadoId, areaDeVidaId, fechaLimite)
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    if (mostrarSelectorFecha) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(fechaLimite ?: DateTimeUtils.inicioDeHoyEpochMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis -> fechaLimite = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis) }
                    mostrarSelectorFecha = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estadoFecha) }
    }
}
