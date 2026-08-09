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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.emojiEstadoActividad
import com.aqpseller.lulaapp.core.ui.emojiTipoActividad
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.RegistroDiario
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

/**
 * Calendario con 3 vistas intercambiables (Día/Semana/Mes, como Google Calendar) que agrupan
 * TODO lo programado — Hábitos, Tareas, Medicamentos, Citas, Fechas importantes — en un solo
 * lugar. Es de solo lectura: tocar una fila lleva a la pantalla donde sí se puede marcar/
 * editar (`onItemClick`), en vez de duplicar esa lógica acá.
 */
@Composable
fun CalendarScreen(
    onItemClick: (ItemAgenda) -> Unit,
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
                TextButton(onClick = viewModel::irAHoy) { Text("Hoy") }
            }
            IconButton(onClick = viewModel::siguiente) { Text("▶", style = MaterialTheme.typography.titleLarge) }
        }

        if (uiState.cargando) return@Column

        when (uiState.modo) {
            ModoCalendario.DIA -> VistaDia(uiState.diasVisibles.firstOrNull(), uiState.registroDelDia, onItemClick)
            ModoCalendario.SEMANA -> VistaSemana(uiState.diasVisibles, onItemClick)
            ModoCalendario.MES -> VistaMes(uiState.diasVisibles, onDiaClick = viewModel::seleccionarFecha)
        }
    }
}

@Composable
private fun VistaDia(dia: DiaCalendarioUi?, registroDelDia: RegistroDiario?, onItemClick: (ItemAgenda) -> Unit) {
    if ((dia == null || dia.items.isEmpty()) && registroDelDia == null) {
        EmptyState(
            emoji = "🌤️",
            titulo = "Nada programado este día.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        registroDelDia?.let { registro ->
            item { TarjetaCierreDelDia(registro) }
        }
        dia?.items?.let { items ->
            items(items, key = { "${it.actividadId}:${it.horario}" }) { item ->
                FilaItemAgenda(item, onClick = { onItemClick(item) })
                HorizontalDivider()
            }
        }
    }
}

/** Solo lectura — resumen de "Cerrar mi día" de esa fecha, si existe (ver `08-decisiones-tecnicas.md`). */
@Composable
private fun TarjetaCierreDelDia(registro: RegistroDiario) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = "🌙 Cierre del día: ${registro.actividadesCompletadas} de ${registro.actividadesTotales}",
            style = MaterialTheme.typography.titleSmall,
        )
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
private fun FilaItemAgenda(item: ItemAgenda, onClick: () -> Unit, compacta: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = if (compacta) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emojiEstadoActividad(item.estado), modifier = Modifier.padding(end = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${emojiTipoActividad(item.tipo)} ${item.nombre}" + (item.horario?.let { " — $it" } ?: ""),
                style = if (compacta) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            )
            item.subtitulo?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
