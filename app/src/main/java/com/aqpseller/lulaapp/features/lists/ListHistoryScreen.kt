package com.aqpseller.lulaapp.features.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.aqpseller.lulaapp.core.ui.EmptyState

/** Historial de "Reiniciar lista" — cada fila es una foto de cómo quedaron los ítems justo antes de reiniciar. */
@Composable
fun ListHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: ListHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var ejecucionExpandidaId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Historial — ${uiState.nombreLista}", style = MaterialTheme.typography.titleLarge)

        if (!uiState.cargando && uiState.ejecuciones.isEmpty()) {
            EmptyState(
                emoji = "📜",
                titulo = "Todavía no hay usos anteriores.",
                subtitulo = "Cada vez que toques \"Reiniciar lista\" queda una foto de cómo quedó acá.",
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(uiState.ejecuciones, key = { it.id }) { ejecucion ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ejecucionExpandidaId = if (ejecucionExpandidaId == ejecucion.id) null else ejecucion.id
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Text(text = ejecucion.fechaTexto, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${ejecucion.marcados} de ${ejecucion.total} completados",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (ejecucionExpandidaId == ejecucion.id) {
                        ejecucion.items.forEach { item ->
                            Text(
                                text = "${if (item.marcado) "✓" else "○"} ${item.texto}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp),
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
