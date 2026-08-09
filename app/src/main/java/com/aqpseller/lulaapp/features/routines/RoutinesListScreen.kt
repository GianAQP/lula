package com.aqpseller.lulaapp.features.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun RoutinesListScreen(
    onRutinaClick: (String) -> Unit,
    onNuevaRutina: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaRutina) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.rutinas.isEmpty()) {
            EmptyState(
                emoji = "🧩",
                titulo = "Todavía no tienes rutinas.",
                subtitulo = "Agrupa hábitos o tareas que sueles hacer juntos (ej. \"Rutina de mañana\").",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Tus rutinas", style = MaterialTheme.typography.titleLarge)

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(uiState.rutinas, key = { it.id }) { rutina ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRutinaClick(rutina.id) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(text = rutina.nombre, style = MaterialTheme.typography.bodyLarge)
                        val progreso = if (rutina.total > 0) rutina.completadas.toFloat() / rutina.total else 0f
                        LulaProgressBar(progreso = progreso, modifier = Modifier.padding(top = 6.dp))
                        Text(
                            text = "${rutina.completadas} de ${rutina.total} hoy",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
