package com.aqpseller.lulaapp.features.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.aqpseller.lulaapp.core.ui.NivelRecordatorioSelector
import com.aqpseller.lulaapp.core.ui.SelectorFechaRapida
import com.aqpseller.lulaapp.core.ui.SelectorRow
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.CategoriaMeta
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

/** Ayuda de referencia para definir una meta — la pregunta a la vez etiqueta la categoría de la
 * meta (`CategoriaMeta`), para poder agrupar más adelante tanto las en progreso como las
 * completadas. Ver `Plan/08-decisiones-tecnicas.md`, 2026-08-01 (antes vivía, mal ubicada,
 * dentro de "Mi propósito"), 2026-08-11 (categoría + ejemplos) y 2026-08-12 (primer paso
 * obligatorio del flujo, formulario compacto tipo Medicamento, a pedido del usuario). */
private fun preguntaAyuda(categoria: CategoriaMeta) = when (categoria) {
    CategoriaMeta.HACER -> "¿Qué quiero hacer?"
    CategoriaMeta.SER -> "¿Qué quiero ser?"
    CategoriaMeta.VER -> "¿Qué quiero ver?"
    CategoriaMeta.TENER -> "¿Qué quiero tener?"
    CategoriaMeta.IR -> "¿Adónde quiero ir?"
    CategoriaMeta.COMPARTIR -> "¿Qué es lo que deseo compartir?"
}

private fun ejemplosAyuda(categoria: CategoriaMeta) = when (categoria) {
    CategoriaMeta.HACER -> listOf("Yo administro mi tiempo haciendo un plan", "Yo gano dinero mientras duermo", "Yo creo empresas y las hago crecer")
    CategoriaMeta.SER -> listOf("Yo soy un ejemplo a seguir", "Yo soy más positivo", "Yo soy mejor cada día")
    CategoriaMeta.VER -> listOf("Yo veo a varios crecer conmigo", "Yo veo la alegría de mi familia", "Yo veo a mis socios más grandes")
    CategoriaMeta.TENER -> listOf("Yo tengo un carro donde llevo a toda mi familia", "Yo tengo casas para alquilar", "Yo tengo a mis trabajadores contentos")
    CategoriaMeta.IR -> listOf("Yo voy de viaje a todo el Perú", "Yo voy a México", "Yo voy al evento de Tomorrowland")
    CategoriaMeta.COMPARTIR -> listOf("Yo comparto mis conocimientos", "Yo comparto momentos inolvidables con mi familia", "Yo comparto sonrisas todos los días")
}

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

private fun etiquetaNivel(nivel: NivelRecordatorio) = when (nivel) {
    NivelRecordatorio.SILENCIOSO -> "🔇 Silencioso"
    NivelRecordatorio.SONIDO -> "🔔 Sonido"
    NivelRecordatorio.ALARMA -> "⏰ Alarma"
}

/** Un mes desde hoy — pedido explícito del usuario: la fecha límite ya no es opcional ("uno
 * debe ponerse una fecha límite"), así que el formulario siempre arranca con un valor real en
 * vez de forzar a elegir uno antes de poder seguir. */
private fun fechaLimitePorDefecto(): Long = DateTimeUtils.localDateAEpochMillis(DateTimeUtils.hoy().plus(DatePeriod(months = 1)))

private enum class SelectorMetaAbierto { MEDIR, AREA, FECHA, RECORDATORIO }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearMetaScreen(
    onGuardado: () -> Unit,
    onVerMetas: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearMetaViewModel = hiltViewModel(),
) {
    var categoria by remember { mutableStateOf<CategoriaMeta?>(null) }
    var mostrarSelectorCategoria by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var comoSeMide by remember { mutableStateOf(ComoSeMideMeta.MANUAL) }
    var objetivoTexto by remember { mutableStateOf("") }
    var habitoVinculadoId by remember { mutableStateOf<String?>(null) }
    var areaDeVidaId by remember { mutableStateOf<String?>(null) }
    var fechaLimite by remember { mutableStateOf(fechaLimitePorDefecto()) }
    var nivelRecordatorio by remember { mutableStateOf(NivelRecordatorio.SONIDO) }
    var selectorAbierto by remember { mutableStateOf<SelectorMetaAbierto?>(null) }

    fun snapshot() = listOf(categoria, nombre, comoSeMide, objetivoTexto, habitoVinculadoId, areaDeVidaId, fechaLimite, nivelRecordatorio)
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
            categoria = it.categoria
            nombre = it.nombre
            comoSeMide = it.comoSeMide
            objetivoTexto = it.valorObjetivo.toString()
            habitoVinculadoId = it.actividadesVinculadasIds.firstOrNull()
            areaDeVidaId = it.areaDeVidaId
            fechaLimite = it.fechaLimite ?: fechaLimitePorDefecto()
            nivelRecordatorio = it.nivelRecordatorio
        }
        snapshotInicial = snapshot()
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = if (viewModel.esEdicion) "Editar meta" else "Nueva meta",
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = onVerMetas, modifier = Modifier.padding(top = 4.dp)) {
            Text("🎯 Ver mis metas")
        }

        Text(text = "Armemos tus metas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Column(modifier = Modifier.padding(top = 8.dp)) {
            HorizontalDivider()
            SelectorRow(
                etiqueta = "Categoría",
                valor = categoria?.let { preguntaAyuda(it) } ?: "Elegir",
                onClick = { mostrarSelectorCategoria = true },
            )
            HorizontalDivider()
        }

        categoria?.let { seleccionada ->
            Text(text = preguntaAyuda(seleccionada), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
            DictationTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = "Respondé acá",
                modifier = Modifier.padding(top = 8.dp),
            )

            Column(modifier = Modifier.padding(top = 16.dp)) {
                HorizontalDivider()
                SelectorRow(
                    etiqueta = "¿Cómo la vas a medir?",
                    valor = etiquetaComoSeMide(comoSeMide),
                    onClick = { selectorAbierto = SelectorMetaAbierto.MEDIR },
                )
                HorizontalDivider()
                SelectorRow(
                    etiqueta = "Área de vida",
                    valor = areasDisponibles.find { it.id == areaDeVidaId }?.nombre ?: "Elegir",
                    onClick = { selectorAbierto = SelectorMetaAbierto.AREA },
                )
                HorizontalDivider()
                SelectorRow(
                    etiqueta = "Fecha límite",
                    valor = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaLimite)),
                    onClick = { selectorAbierto = SelectorMetaAbierto.FECHA },
                )
                HorizontalDivider()
                SelectorRow(
                    etiqueta = "Recordatorio",
                    valor = etiquetaNivel(nivelRecordatorio),
                    onClick = { selectorAbierto = SelectorMetaAbierto.RECORDATORIO },
                )
                HorizontalDivider()
            }

            Button(
                onClick = {
                    viewModel.guardar(
                        nombre, comoSeMide, objetivoTexto.toDoubleOrNull() ?: 0.0, habitoVinculadoId, areaDeVidaId,
                        fechaLimite, categoria, nivelRecordatorio,
                    )
                },
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
            }
        }
    }

    if (mostrarSelectorCategoria) {
        SelectorCategoriaSheet(
            categoriaSeleccionada = categoria,
            onCategoriaSeleccionada = { categoria = it },
            onCerrar = { mostrarSelectorCategoria = false },
        )
    }

    when (selectorAbierto) {
        SelectorMetaAbierto.MEDIR -> {
            SelectorComoSeMideSheet(
                esEdicion = viewModel.esEdicion,
                comoSeMide = comoSeMide,
                onComoSeMideCambiado = { comoSeMide = it; habitoVinculadoId = null },
                habitoVinculadoId = habitoVinculadoId,
                onHabitoVinculadoCambiado = { habitoVinculadoId = it },
                habitosDisponibles = habitosDisponibles,
                objetivoTexto = objetivoTexto,
                onObjetivoCambiado = { objetivoTexto = it },
                onCerrar = { selectorAbierto = null },
            )
        }
        SelectorMetaAbierto.AREA -> {
            SelectorAreaSheet(
                areasDisponibles = areasDisponibles,
                areaDeVidaId = areaDeVidaId,
                onAreaCambiada = { areaDeVidaId = it },
                onCerrar = { selectorAbierto = null },
            )
        }
        SelectorMetaAbierto.FECHA -> {
            SelectorFechaMetaSheet(
                fechaLimite = fechaLimite,
                onFechaCambiada = { fechaLimite = it; selectorAbierto = null },
                onCerrar = { selectorAbierto = null },
            )
        }
        SelectorMetaAbierto.RECORDATORIO -> {
            SelectorRecordatorioMetaSheet(
                nivelRecordatorio = nivelRecordatorio,
                onNivelCambiado = { nivelRecordatorio = it },
                onCerrar = { selectorAbierto = null },
            )
        }
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCategoriaSheet(
    categoriaSeleccionada: CategoriaMeta?,
    onCategoriaSeleccionada: (CategoriaMeta) -> Unit,
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
            Text(text = "¿A cuál de estas preguntas responde tu meta?", style = MaterialTheme.typography.titleMedium)
            CategoriaMeta.entries.forEach { opcion ->
                val seleccionadaAhora = categoriaSeleccionada == opcion
                FilterChip(
                    selected = seleccionadaAhora,
                    onClick = {
                        onCategoriaSeleccionada(opcion)
                        if (!seleccionadaAhora) return@FilterChip
                        onCerrar()
                    },
                    label = { Text(preguntaAyuda(opcion)) },
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (seleccionadaAhora) {
                    ejemplosAyuda(opcion).forEach { ejemplo ->
                        Text(text = "· $ejemplo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 8.dp))
                    }
                }
            }
            Text(
                text = "Consejos para redactarla",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            CONSEJOS_REDACCION_META.forEach { consejo ->
                Text(text = "· $consejo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            Button(onClick = onCerrar, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                Text("Listo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorComoSeMideSheet(
    esEdicion: Boolean,
    comoSeMide: ComoSeMideMeta,
    onComoSeMideCambiado: (ComoSeMideMeta) -> Unit,
    habitoVinculadoId: String?,
    onHabitoVinculadoCambiado: (String) -> Unit,
    habitosDisponibles: List<Actividad>,
    objetivoTexto: String,
    onObjetivoCambiado: (String) -> Unit,
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
            Text(text = "¿Cómo la vas a medir?", style = MaterialTheme.typography.titleMedium)
            if (esEdicion) {
                Text(
                    text = etiquetaComoSeMide(comoSeMide),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = "No se puede cambiar después de creada la meta.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                FlowRow(modifier = Modifier.padding(top = 12.dp)) {
                    ComoSeMideMeta.entries.forEach { opcion ->
                        FilterChip(
                            selected = comoSeMide == opcion,
                            onClick = { onComoSeMideCambiado(opcion) },
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
                                onClick = { onHabitoVinculadoCambiado(habito.id) },
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
                        "eso ya lo escribiste como nombre.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            OutlinedTextField(
                value = objetivoTexto,
                onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == '.' }) onObjetivoCambiado(nuevo) },
                label = { Text(etiquetaObjetivo(comoSeMide)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            Button(onClick = onCerrar, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                Text("Listo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorAreaSheet(
    areasDisponibles: List<AreaDeVida>,
    areaDeVidaId: String?,
    onAreaCambiada: (String?) -> Unit,
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
            Text(text = "Área de vida (opcional)", style = MaterialTheme.typography.titleMedium)
            if (areasDisponibles.isEmpty()) {
                Text(
                    text = "Todavía no tienes áreas de vida configuradas.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                FlowRow(modifier = Modifier.padding(top = 12.dp)) {
                    areasDisponibles.forEach { area ->
                        FilterChip(
                            selected = areaDeVidaId == area.id,
                            onClick = { onAreaCambiada(if (areaDeVidaId == area.id) null else area.id) },
                            label = { Text(area.nombre) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                        )
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
private fun SelectorFechaMetaSheet(
    fechaLimite: Long,
    onFechaCambiada: (Long) -> Unit,
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
            Text(text = "¿Cuándo la querés cumplir?", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Se puede aplazar después si hace falta más tiempo.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "📅 " + DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaLimite)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            SelectorFechaRapida(
                fechaActual = fechaLimite,
                onFechaElegida = onFechaCambiada,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onCerrar, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                Text("Listo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRecordatorioMetaSheet(
    nivelRecordatorio: NivelRecordatorio,
    onNivelCambiado: (NivelRecordatorio) -> Unit,
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
            Text(
                text = "Avisa a las 9 de la mañana del día que llega la fecha límite.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            NivelRecordatorioSelector(
                nivelSeleccionado = nivelRecordatorio,
                onNivelSeleccionado = onNivelCambiado,
                modifier = Modifier.padding(top = 12.dp),
            )
            Button(onClick = onCerrar, modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                Text("Listo")
            }
        }
    }
}
