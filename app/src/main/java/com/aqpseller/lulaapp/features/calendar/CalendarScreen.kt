package com.aqpseller.lulaapp.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.emojiEstadoActividad
import com.aqpseller.lulaapp.core.ui.emojiTipoActividad
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.model.TipoActividad
import kotlinx.datetime.LocalDate

private fun etiquetaModo(modo: ModoCalendario): String = when (modo) {
    ModoCalendario.DIA -> "Día"
    ModoCalendario.SEMANA -> "Semana"
    ModoCalendario.MES -> "Mes"
}

private val LETRAS_DIA_SEMANA = listOf("L", "M", "M", "J", "V", "S", "D")

private fun tituloRango(uiState: CalendarUiState): String = when (uiState.modo) {
    ModoCalendario.DIA -> DateTimeUtils.formatearFechaLarga(uiState.fechaSeleccionada)
    ModoCalendario.SEMANA -> {
        val inicio = uiState.diasVisibles.firstOrNull()?.fecha ?: uiState.fechaSeleccionada
        val fin = uiState.diasVisibles.lastOrNull()?.fecha ?: uiState.fechaSeleccionada
        "${inicio.dayOfMonth} al ${DateTimeUtils.formatearFechaLarga(fin)}"
    }
    ModoCalendario.MES -> DateTimeUtils.formatearMesYAnio(uiState.fechaSeleccionada)
}

private fun viendoHoy(uiState: CalendarUiState): Boolean = uiState.diasVisibles.any { it.esHoy }

/** Solo tiene sentido para un día puntual (Semana/Mes muestran un rango, no un solo día de la
 * semana) — antes esto era un botón fijo con el texto "Hoy" siempre visible sin importar la
 * fecha, confuso porque parecía describir el día mostrado. Ver `08-decisiones-tecnicas.md`. */
private fun subtituloDia(uiState: CalendarUiState): String? {
    if (uiState.modo != ModoCalendario.DIA) return null
    val nombreDia = DateTimeUtils.nombreDiaIso(DateTimeUtils.numeroDiaIso(uiState.fechaSeleccionada))
        .replaceFirstChar { it.uppercase() }
    return if (viendoHoy(uiState)) "Hoy, $nombreDia" else nombreDia
}

/**
 * Calendario con 3 vistas intercambiables (Día/Semana/Mes, como Google Calendar) que agrupan
 * TODO lo programado — Hábitos, Tareas, Medicamentos, Citas, Fechas importantes — en un solo
 * lugar. Es de solo lectura: tocar una fila lleva a la pantalla donde sí se puede marcar/
 * editar (`onItemClick`), en vez de duplicar esa lógica acá.
 */
@Composable
fun CalendarScreen(
    onItemClick: (ItemAgenda) -> Unit,
    onCerrarDia: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Calendario", style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.padding(top = 12.dp)) {
            ModoCalendario.entries.forEach { modo ->
                FilterChip(
                    selected = uiState.modo == modo,
                    onClick = { viewModel.cambiarModo(modo) },
                    label = { Text(etiquetaModo(modo)) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = viewModel::anterior) { Text("◀", style = MaterialTheme.typography.titleLarge) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = tituloRango(uiState), style = MaterialTheme.typography.titleMedium)
                subtituloDia(uiState)?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                if (!viendoHoy(uiState)) {
                    TextButton(onClick = viewModel::irAHoy) { Text("Ir a hoy") }
                }
            }
            IconButton(onClick = viewModel::siguiente) { Text("▶", style = MaterialTheme.typography.titleLarge) }
        }

        if (uiState.cargando) return@Column

        when (uiState.modo) {
            ModoCalendario.DIA -> VistaDia(
                uiState.diasVisibles.firstOrNull(),
                uiState.registroDelDia,
                onItemClick,
                onCerrarDia,
                onMarcarTareaEnFecha = viewModel::marcarTareaEnFecha,
            )
            ModoCalendario.SEMANA -> VistaSemana(uiState.diasVisibles, onItemClick)
            ModoCalendario.MES -> VistaMes(uiState.diasVisibles, onDiaClick = viewModel::seleccionarFecha)
        }
    }
}

@Composable
private fun VistaDia(
    dia: DiaCalendarioUi?,
    registroDelDia: RegistroDiario?,
    onItemClick: (ItemAgenda) -> Unit,
    onCerrarDia: (Long) -> Unit,
    onMarcarTareaEnFecha: (String, Long) -> Unit,
) {
    if ((dia == null || dia.items.isEmpty()) && registroDelDia == null) {
        EmptyState(
            emoji = "🌤️",
            titulo = "Nada programado este día.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    val fechaEpochDay = dia?.fecha?.toEpochDays()?.toLong()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        if (registroDelDia != null) {
            item { TarjetaCierreDelDia(registroDelDia, onEditar = { fechaEpochDay?.let(onCerrarDia) }) }
        } else if (dia != null && !dia.esHoy && dia.fecha < DateTimeUtils.hoy()) {
            // Solo se ofrece para días pasados sin cerrar — hoy ya tiene su propio botón fijo
            // "Cerrar mi día", y un día futuro todavía no pasó, no hay nada que registrar.
            item {
                TextButton(onClick = { fechaEpochDay?.let(onCerrarDia) }, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("📝 Llenar el cierre de este día")
                }
            }
        }
        dia?.items?.let { items ->
            // Solo Tarea, y solo en un día pasado, se puede marcar con la fecha real en que se
            // hizo — Hábito ya se marca por día como corresponde, y un día futuro no tiene nada
            // que marcar todavía. A pedido del usuario: la única forma de completar una tarea
            // atrasada "en su día" es entrando acá. Ver `Plan/08-decisiones-tecnicas.md`.
            val esDiaPasado = !dia.esHoy && dia.fecha < DateTimeUtils.hoy()
            items(items, key = { "${it.actividadId}:${it.horario}" }) { item ->
                val puedeMarcarAtrasada = esDiaPasado && item.tipo == TipoActividad.TAREA && item.estado == EstadoActividad.SIN_CONFIRMAR
                FilaItemAgenda(
                    item,
                    onClick = { onItemClick(item) },
                    onMarcarHecha = if (puedeMarcarAtrasada) {
                        { fechaEpochDay?.let { onMarcarTareaEnFecha(item.actividadId, it) } }
                    } else {
                        null
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

/** Resumen de "Cerrar mi día" de esa fecha, si existe — con opción de editarlo (ver `Plan/08-decisiones-tecnicas.md`). */
@Composable
private fun TarjetaCierreDelDia(registro: RegistroDiario, onEditar: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🌙 Cierre del día: ${registro.actividadesCompletadas} de ${registro.actividadesTotales}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEditar) { Text("✏️ Editar") }
        }
        registro.queLogre?.let { Text(text = "Logré: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        registro.queCosto?.let { Text(text = "Costó: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        registro.queAjusto?.let { Text(text = "Ajusté: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun VistaSemana(dias: List<DiaCalendarioUi>, onItemClick: (ItemAgenda) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        dias.forEach { dia ->
            item {
                Text(
                    text = (if (dia.esHoy) "🔵 " else "") + DateTimeUtils.formatearFechaLarga(dia.fecha),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (dia.items.isEmpty()) {
                item {
                    Text(
                        text = "Nada programado",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            } else {
                items(dia.items, key = { "${dia.fecha}:${it.actividadId}:${it.horario}" }) { item ->
                    FilaItemAgenda(item, onClick = { onItemClick(item) }, compacta = true)
                }
            }
        }
    }
}

@Composable
private fun VistaMes(dias: List<DiaCalendarioUi>, onDiaClick: (LocalDate) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            LETRAS_DIA_SEMANA.forEach { letra ->
                Text(
                    text = letra,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            gridItems(dias, key = { it.fecha.toString() }) { dia ->
                CeldaDia(dia, onClick = { onDiaClick(dia.fecha) })
            }
        }
    }
}

@Composable
private fun CeldaDia(dia: DiaCalendarioUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(onClick = onClick)
            .let { if (dia.esHoy) it.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else it }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dia.fecha.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (dia.esDelMesVisible) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (dia.items.isNotEmpty()) {
            Text(text = "•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FilaItemAgenda(
    item: ItemAgenda,
    onClick: () -> Unit,
    compacta: Boolean = false,
    onMarcarHecha: (() -> Unit)? = null,
) {
    // Un eliminado no lleva a ningún detalle (ya no existe) ni se puede marcar — solo queda como
    // rastro de que existió y se borró. Se ve apagado a propósito para no competir visualmente
    // con lo que sigue vigente. Ver `Plan/08-decisiones-tecnicas.md`.
    val colorTexto = if (item.eliminado) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (item.eliminado) it else it.clickable(onClick = onClick) }
            .padding(vertical = if (compacta) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (item.eliminado) "🗑️" else emojiEstadoActividad(item.estado),
            modifier = Modifier.padding(end = 8.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${emojiTipoActividad(item.tipo)} ${item.nombre}" + (item.horario?.let { " — $it" } ?: ""),
                style = if (compacta) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = colorTexto,
            )
            item.subtitulo?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colorTexto) }
        }
        if (onMarcarHecha != null) {
            Checkbox(checked = false, onCheckedChange = { onMarcarHecha() })
        }
    }
}
