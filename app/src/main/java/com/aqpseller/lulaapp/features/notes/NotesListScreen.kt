package com.aqpseller.lulaapp.features.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.EmptyState

@Composable
fun NotesListScreen(
    onNotaClick: (String) -> Unit,
    onNuevaNota: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var notaAEliminar by remember { mutableStateOf<NotaListItemUi?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaNota) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.notas.isEmpty()) {
            EmptyState(
                emoji = "📝",
                titulo = "Todavía no tienes notas.",
                subtitulo = "Toca + para escribir la primera.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Notas", style = MaterialTheme.typography.titleLarge)

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                itemsIndexed(uiState.notas, key = { _, nota -> nota.id }) { index, nota ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNotaClick(nota.id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(text = nota.titulo, style = MaterialTheme.typography.bodyLarge)
                            if (nota.preview.isNotBlank()) {
                                Text(text = nota.preview, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(text = nota.fechaTexto, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { viewModel.moverArriba(nota.id) }, enabled = index > 0) {
                            Text("▲")
                        }
                        IconButton(onClick = { viewModel.moverAbajo(nota.id) }, enabled = index < uiState.notas.lastIndex) {
                            Text("▼")
                        }
                        IconButton(onClick = { notaAEliminar = nota }) {
                            Text("🗑️")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    notaAEliminar?.let { nota ->
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina \"${nota.titulo}\" para siempre.",
            onConfirmar = { viewModel.eliminar(nota.id); notaAEliminar = null },
            onCancelar = { notaAEliminar = null },
        )
    }
}
