package com.aqpseller.lulaapp.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.DatePeriod

private fun tituloGrupo(fecha: LocalDate): String = when (fecha) {
    DateTimeUtils.hoy() -> "Hoy"
    DateTimeUtils.hoy().minus(DatePeriod(days = 1)) -> "Ayer"
    else -> DateTimeUtils.formatearFechaLarga(fecha)
}

@Composable
fun NotificacionesScreen(
    onAbrirCirculoDeCuidado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificacionesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var soloNoLeidas by remember { mutableStateOf(false) }
    if (uiState.cargando) return

    val visibles = if (soloNoLeidas) uiState.notificaciones.filter { !it.leido } else uiState.notificaciones
    val agrupadas = visibles.groupBy { DateTimeUtils.epochMillisToLocalDate(it.fecha) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            FilterChip(
                selected = !soloNoLeidas,
                onClick = { soloNoLeidas = false },
                label = { Text("Todo") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = soloNoLeidas,
                onClick = { soloNoLeidas = true },
                label = { Text("No leído") },
            )
        }

        if (visibles.isEmpty()) {
            EmptyState(
                emoji = "🔔",
                titulo = if (soloNoLeidas) "No tienes notificaciones sin leer." else "Todavía no tienes notificaciones.",
                modifier = Modifier.fillMaxSize(),
            )
            return
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            agrupadas.forEach { (fecha, items) ->
                item {
                    Text(
                        text = tituloGrupo(fecha),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(items, key = { it.id }) { item ->
                    FilaNotificacion(
                        item = item,
                        onClick = {
                            viewModel.marcarLeida(item.id)
                            if (item.solicitudId != null) onAbrirCirculoDeCuidado()
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FilaNotificacion(item: NotificacionItemUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(end = 12.dp)) {
            Text(text = item.emoji, style = MaterialTheme.typography.headlineSmall)
            if (!item.leido) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.titulo,
                style = if (item.leido) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleSmall,
            )
            Text(
                text = item.cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = DateTimeUtils.horaHHmm(item.fecha),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
