package com.aqpseller.lulaapp.features.family

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState

@Composable
fun RetosFamiliaresScreen(
    onCrearReto: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RetosFamiliaresViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.cargando) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "🏆 Retos familiares",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )

        if (uiState.retos.isEmpty()) {
            EmptyState(
                emoji = "🏆",
                titulo = "Todavía no hay retos familiares.",
                textoBoton = "+ Crear reto familiar",
                onBotonClick = onCrearReto,
                modifier = Modifier.padding(bottom = 64.dp),
            )
            return
        }

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            items(uiState.retos, key = { it.id }) { reto ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = reto.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(text = reto.objetivo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        reto.recompensa?.let {
                            Text(text = "🎁 $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                        Text(
                            text = "Progreso: ${reto.cumplidosHoy} de ${reto.totalParticipantes} ya cumplieron hoy",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = reto.yoCumpliHoy,
                                onCheckedChange = { marcado -> viewModel.marcarCumplidoHoy(reto.id, marcado) },
                            )
                            Text(text = "Yo ya cumplí hoy")
                        }
                    }
                }
            }
            item {
                Button(onClick = onCrearReto, modifier = Modifier.padding(bottom = 16.dp)) {
                    Text("+ Crear reto familiar")
                }
            }
        }
    }
}
