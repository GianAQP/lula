package com.aqpseller.lulaapp.features.lists

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
fun ListsScreen(
    onListaClick: (String) -> Unit,
    onNuevaLista: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaLista) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.listas.isEmpty()) {
            EmptyState(
                emoji = "📋",
                titulo = "Todavía no tienes listas.",
                subtitulo = "Crea una para cosas que preparas de vez en cuando (ej. \"Viaje\", \"Compras\") y reutilízala cada vez.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Tus listas", style = MaterialTheme.typography.titleLarge)

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(uiState.listas, key = { it.id }) { lista ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onListaClick(lista.id) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(text = lista.nombre, style = MaterialTheme.typography.bodyLarge)
                        val progreso = if (lista.total > 0) lista.marcados.toFloat() / lista.total else 0f
                        LulaProgressBar(progreso = progreso, modifier = Modifier.padding(top = 6.dp))
                        Text(
                            text = "${lista.marcados} de ${lista.total}",
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
