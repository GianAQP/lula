package com.aqpseller.lulaapp.features.lists

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.LulaProgressBar
import com.aqpseller.lulaapp.core.ui.SonidoCheckViewModel
import com.aqpseller.lulaapp.core.utils.QrCodeGenerator
import com.aqpseller.lulaapp.core.utils.SonidoUtils
import com.aqpseller.lulaapp.core.utils.codificarListaQr
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var nuevoItem by remember { mutableStateOf("") }
    var itemPendienteEliminar by remember { mutableStateOf<ListaItem?>(null) }
    var mostrarConfirmacionLista by remember { mutableStateOf(false) }
    var mostrarQr by remember { mutableStateOf(false) }
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

        // Compartir como texto plano — sirve incluso si la otra persona no tiene Lula instalada
        // (WhatsApp, correo, lo que sea). No queda ningún vínculo entre las dos listas después:
        // es una copia de una sola vez, no seguimiento en vivo — eso último necesita cuentas
        // reales y queda pendiente (ver `Plan/08-decisiones-tecnicas.md`). A pedido del usuario.
        Row(modifier = Modifier.padding(top = 24.dp)) {
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(textoDeLista(uiState.nombre, uiState.items)))
                    Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(end = 8.dp),
            ) { Text("📋 Copiar") }
            OutlinedButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textoDeLista(uiState.nombre, uiState.items))
                }
                runCatching { context.startActivity(Intent.createChooser(intent, "Compartir lista")) }
            }) { Text("📤 Compartir") }
            OutlinedButton(onClick = { mostrarQr = true }, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Por código")
            }
        }

        Row(modifier = Modifier.padding(top = 12.dp)) {
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
    if (mostrarQr) {
        val textoQr = codificarListaQr(uiState.nombre, uiState.items.map { it.texto })
        val qr = remember(textoQr) { QrCodeGenerator.generar(textoQr) }
        AlertDialog(
            onDismissRequest = { mostrarQr = false },
            confirmButton = { TextButton(onClick = { mostrarQr = false }) { Text("Listo") } },
            title = { Text("Que la otra persona escanee este código") },
            text = {
                Column {
                    Text(
                        text = "Desde Listas en su Lula, con el botón \"📷\" — se crea una copia " +
                            "en su teléfono, sin quedar vinculada con esta.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    qr?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(220.dp)) }
                }
            },
        )
    }
}

private fun textoDeLista(nombre: String, items: List<ListaItem>): String {
    val lineas = items.joinToString("\n") { item -> "${if (item.marcado) "☑" else "☐"} ${item.texto}" }
    return "📋 $nombre\n\n$lineas"
}
