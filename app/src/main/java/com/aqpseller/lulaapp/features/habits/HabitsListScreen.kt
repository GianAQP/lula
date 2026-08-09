package com.aqpseller.lulaapp.features.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.etiquetaMomentoDelDia
import com.aqpseller.lulaapp.domain.model.MomentoDelDia

@Composable
fun HabitsListScreen(
    onHabitoClick: (String) -> Unit,
    onNuevoHabito: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoHabito) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.habitos.isEmpty()) {
            EmptyState(
                emoji = "🌿",
                titulo = "Todavía no tienes hábitos.",
                subtitulo = "Toca + para crear el primero, poco a poco.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp)) {
            Text(text = "Tus hábitos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
            if (uiState.habitos.isNotEmpty()) {
                Text(
                    text = uiState.mensajeMotivacional,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                MomentoDelDia.entries.forEach { momento ->
                    val habitosDelMomento = uiState.porMomento[momento].orEmpty()
                    if (habitosDelMomento.isNotEmpty()) {
                        item(key = "titulo_$momento") {
                            Text(
                                text = etiquetaMomentoDelDia(momento).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(habitosDelMomento, key = { it.id }) { habito ->
                            TarjetaHabito(habito = habito, onClick = { onHabitoClick(habito.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaHabito(habito: HabitoListItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${habito.emoji} ${habito.nombre}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(text = "🔥 ${habito.racha}", style = MaterialTheme.typography.bodyMedium)
            }
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                habito.diasSemana.forEach { dia ->
                    Text(
                        text = dia.letra,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (dia.esHoy) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                habito.diasSemana.forEach { dia -> CirculoDia(dia) }
            }
        }
    }
}

@Composable
private fun CirculoDia(dia: DiaTrackerUi, modifier: Modifier = Modifier) {
    val colorRelleno = if (dia.confirmado) MaterialTheme.colorScheme.primary else Color.Transparent
    val colorBorde = if (dia.esHoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .then(
                if (dia.confirmado) {
                    Modifier.background(colorRelleno)
                } else {
                    Modifier.border(width = if (dia.esHoy) 2.dp else 1.dp, color = colorBorde, shape = CircleShape)
                },
            ),
    )
}
