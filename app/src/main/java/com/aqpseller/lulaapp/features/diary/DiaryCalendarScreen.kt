package com.aqpseller.lulaapp.features.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import kotlinx.datetime.LocalDate

private val LETRAS_DIA_SEMANA = listOf("L", "M", "M", "J", "V", "S", "D")

/**
 * Grilla mensual del Diario — cada día marcado (fondo + 📝) si ya tiene entrada, vacío si no.
 * Tocar un día lleva a su entrada existente, o abre una nueva ya fechada ese día — así se
 * puede ver de un vistazo qué días quedaron sin llenar y elegir cuál completar.
 */
@Composable
fun DiaryCalendarScreen(
    onDiaClick: (fecha: LocalDate, entradaId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryCalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Calendario del diario", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = viewModel::mesAnterior) { Text("◀", style = MaterialTheme.typography.titleLarge) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = DateTimeUtils.formatearMesYAnio(uiState.mesVisible), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::irAHoy) { Text("Hoy") }
            }
            IconButton(onClick = viewModel::mesSiguiente) { Text("▶", style = MaterialTheme.typography.titleLarge) }
        }

        if (uiState.cargando) return@Column

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
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
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            items(uiState.dias, key = { it.fecha.toString() }) { dia ->
                CeldaDiaDiario(dia, onClick = { onDiaClick(dia.fecha, dia.entradaId) })
            }
        }
    }
}

@Composable
private fun CeldaDiaDiario(dia: DiaDiarioUi, onClick: () -> Unit) {
    val tieneEntrada = dia.entradaId != null
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable(onClick = onClick)
            .let {
                when {
                    dia.esHoy -> it.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    tieneEntrada -> it.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    else -> it
                }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
        Text(text = if (tieneEntrada) "📝" else " ", style = MaterialTheme.typography.labelSmall)
    }
}
