package com.aqpseller.lulaapp.features.habits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitDetailScreen(
    onEditar: (String) -> Unit,
    onEliminado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.eliminado) { if (uiState.eliminado) onEliminado() }
    LaunchedEffect(Unit) { viewModel.recargar() }

    if (uiState.cargando) return

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = uiState.nombre, style = MaterialTheme.typography.titleLarge)
        Text(text = "Momento: ${uiState.momentoDelDia?.name.orEmpty()}", modifier = Modifier.padding(top = 8.dp))
        Text(text = "🔥 Racha actual: ${uiState.racha} días", modifier = Modifier.padding(top = 8.dp))
        if (!uiState.activa) {
            Text(text = "Pausado", modifier = Modifier.padding(top = 8.dp))
        }

        Text(text = "Historial (últimos 30 días)", modifier = Modifier.padding(top = 16.dp))
        FlowRow(modifier = Modifier.padding(top = 8.dp)) {
            uiState.diasHistorial30.forEach { confirmado ->
                Text(text = if (confirmado) "●" else "○", modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
            }
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            Button(onClick = { onEditar(viewModel.actividadId) }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Editar")
            }
            OutlinedButton(onClick = viewModel::pausarOReanudar, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (uiState.activa) "Pausar" else "Reanudar")
            }
            OutlinedButton(onClick = { mostrarConfirmacion = true }) {
                Text("Eliminar")
            }
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina \"${uiState.nombre}\" y su historial para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminar() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }
}
