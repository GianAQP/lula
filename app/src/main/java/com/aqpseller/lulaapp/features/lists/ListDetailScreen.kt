package com.aqpseller.lulaapp.features.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.LulaProgressBar
import com.aqpseller.lulaapp.core.ui.SonidoCheckViewModel
import com.aqpseller.lulaapp.core.utils.SonidoUtils
import com.aqpseller.lulaapp.domain.model.ListaItem

@Composable
fun ListDetailScreen(
    onEliminada: () -> Unit,
    onVerHistorial: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListDetailViewModel = hiltViewModel(),
    sonidoCheckViewModel: SonidoCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sonidoCheckHabilitado by sonidoCheckViewModel.habilitado.collectAsState()
    var nuevoItem by remember { mutableStateOf("") }
    var itemPendienteEliminar by remember { mutableStateOf<ListaItem?>(null) }
    var mostrarConfirmacionLista by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.eliminada) { if (uiState.eliminada) onEliminada() }

    if (uiState.cargando) return

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(text = uiState.nombre, style = MaterialTheme.typography.titleLarge)

        val marcados = uiState.items.count { it.marcado }
        Text(
            text = "$marcados de ${uiState.items.size}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        val progreso = if (uiState.items.isNotEmpty()) marcados.toFloat() / uiState.items.size else 0f
        LulaProgressBar(progreso = progreso, modifier = Modifier.padding(top = 8.dp))

        uiState.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = item.marcado,
                    onCheckedChange = { marcado ->
                        viewModel.marcarItem(item.id, marcado)
                        if (sonidoCheckHabilitado) SonidoUtils.reproducirCheck()
                    },
                )
                Text(
                    text = item.texto,
                    textDecoration = if (item.marcado) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { itemPendienteEliminar = item }) {
                    Text("✕")
                }
            }
        }

        Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            DictationTextField(
                value = nuevoItem,
                onValueChange = { nuevoItem = it },
                label = "Agregar ítem",
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                viewModel.agregarItem(nuevoItem)
                nuevoItem = ""
            }) {
                Text("+")
            }
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            OutlinedButton(onClick = viewModel::reiniciar, modifier = Modifier.padding(end = 8.dp)) {
                Text("🔄 Reiniciar lista")
            }
            OutlinedButton(onClick = { mostrarConfirmacionLista = true }) {
                Text("Eliminar lista")
            }
        }
        TextButton(onClick = { onVerHistorial(viewModel.listaId) }, modifier = Modifier.padding(top = 4.dp)) {
            Text("📜 Ver historial de usos anteriores")
        }
    }

    itemPendienteEliminar?.let { item ->
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina \"${item.texto}\" de la lista para siempre.",
            onConfirmar = { viewModel.eliminarItem(item.id); itemPendienteEliminar = null },
            onCancelar = { itemPendienteEliminar = null },
        )
    }
    if (mostrarConfirmacionLista) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina la lista \"${uiState.nombre}\" y todos sus ítems para siempre.",
            onConfirmar = { mostrarConfirmacionLista = false; viewModel.eliminarLista() },
            onCancelar = { mostrarConfirmacionLista = false },
        )
    }
}
