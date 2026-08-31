package com.aqpseller.lulaapp.features.diary

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun DiaryListScreen(
    onEntradaClick: (String) -> Unit,
    onNuevaEntrada: () -> Unit,
    onVerCalendario: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var entradaAEliminar by remember { mutableStateOf<DiaryEntryItemUi?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaEntrada) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.entradas.isEmpty() && uiState.consulta.isBlank()) {
            EmptyState(
                emoji = "📓",
                titulo = "Todavía no tienes entradas en tu diario.",
                subtitulo = "Toca + para escribir la primera.",
                textoBoton = "📅 Ver calendario",
                onBotonClick = onVerCalendario,
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Diario", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onVerCalendario, modifier = Modifier.padding(top = 4.dp)) {
                Text("📅 Ver calendario")
            }
            OutlinedTextField(
                value = uiState.consulta,
                onValueChange = viewModel::buscar,
                label = { Text("🔎 Buscar en tu diario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            if (uiState.entradas.isEmpty()) {
                Text(
                    text = "No encontré nada con \"${uiState.consulta}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(uiState.entradas, key = { it.id }) { entrada ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEntradaClick(entrada.id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(text = entrada.fechaTexto, style = MaterialTheme.typography.bodyLarge)
                            if (entrada.extracto.isNotBlank()) {
                                Text(text = entrada.extracto, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { entradaAEliminar = entrada }) {
                            Text("🗑️")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    entradaAEliminar?.let { entrada ->
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina la entrada del ${entrada.fechaTexto} para siempre.",
            onConfirmar = { viewModel.eliminar(entrada.id); entradaAEliminar = null },
            onCancelar = { entradaAEliminar = null },
        )
    }
}
