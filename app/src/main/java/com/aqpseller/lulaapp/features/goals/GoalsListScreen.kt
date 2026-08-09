package com.aqpseller.lulaapp.features.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.LulaProgressBar
import com.aqpseller.lulaapp.core.utils.DateTimeUtils

@Composable
fun GoalsListScreen(
    onMetaClick: (String) -> Unit,
    onNuevaMeta: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaMeta) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.metas.isEmpty()) {
            EmptyState(
                emoji = "🎯",
                titulo = "Todavía no tienes metas.",
                subtitulo = "Toca + para crear la primera, poco a poco se llega.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Tus metas", style = MaterialTheme.typography.titleLarge)

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                itemsIndexed(uiState.metas, key = { _, meta -> meta.id }) { index, meta ->
                    // La lista ya viene ordenada con las completadas al final — un solo
                    // separador justo antes de la primera marca el quiebre, sin duplicar por
                    // cada fila.
                    val esPrimeraCompletada = meta.fraccionProgreso >= 1f &&
                        (index == 0 || uiState.metas[index - 1].fraccionProgreso < 1f)
                    if (esPrimeraCompletada) {
                        Text(
                            text = "✅ Completadas",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMetaClick(meta.id) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(text = meta.nombre, style = MaterialTheme.typography.bodyLarge)
                        LulaProgressBar(progreso = meta.fraccionProgreso, modifier = Modifier.padding(top = 6.dp))
                        Text(
                            text = "${meta.progreso.toInt()} de ${meta.objetivo.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        meta.nombreHabitoVinculado?.let {
                            Text(text = "Hábito vinculado: $it", style = MaterialTheme.typography.bodySmall)
                        }
                        meta.fechaLimite?.let { fechaLimite ->
                            Text(
                                text = "📅 ${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaLimite))}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
