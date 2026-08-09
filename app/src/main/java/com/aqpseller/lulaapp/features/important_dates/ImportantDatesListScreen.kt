package com.aqpseller.lulaapp.features.important_dates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun ImportantDatesListScreen(
    onFechaClick: (String) -> Unit,
    onNuevaFecha: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportantDatesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var fechaAEliminar by remember { mutableStateOf<FechaImportanteListItemUi?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaFecha) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.fechas.isEmpty()) {
            EmptyState(
                emoji = "🎉",
                titulo = "Todavía no tienes fechas importantes.",
                subtitulo = "Toca + para agregar un cumpleaños, aniversario o lo que quieras recordar.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Fechas importantes", style = MaterialTheme.typography.titleLarge)

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(uiState.fechas, key = { it.id }) { fecha ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onFechaClick(fecha.id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(text = "🎂 ${fecha.nombre}", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${fecha.fechaTexto} · ${fecha.recurrenciaTexto}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { fechaAEliminar = fecha }) {
                            Text("🗑️")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    fechaAEliminar?.let { fecha ->
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina \"${fecha.nombre}\" para siempre.",
            onConfirmar = { viewModel.eliminar(fecha.id); fechaAEliminar = null },
            onCancelar = { fechaAEliminar = null },
        )
    }
}
