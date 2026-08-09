package com.aqpseller.lulaapp.features.notes

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.DictationTextField

/**
 * A propósito **no** usa `verticalScroll` como el resto de las pantallas de formulario — acá
 * el campo de texto es el protagonista y ya scrollea internamente (`singleLine = false` +
 * `weight(1f)`); envolverlo en un `Column` con scroll externo le quita el límite de altura
 * que necesita para no crecer sin fin y empujar los botones fuera de la pantalla.
 */
@Composable
fun NoteEditorScreen(
    onGuardado: () -> Unit,
    onEliminada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val guardado by viewModel.guardado.collectAsState()
    val eliminada by viewModel.eliminada.collectAsState()
    val formInicial by viewModel.formInicial.collectAsState()
    val mensajeError by viewModel.mensajeError.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    LaunchedEffect(eliminada) { if (eliminada) onEliminada() }
    LaunchedEffect(formInicial) {
        formInicial?.let {
            titulo = it.titulo.orEmpty()
            contenido = it.contenido
        }
    }
    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.errorMostrado()
        }
    }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text(
            text = if (viewModel.esEdicion) "Editar nota" else "Nueva nota",
            style = MaterialTheme.typography.titleLarge,
        )

        DictationTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = "Título (opcional)",
            modifier = Modifier.padding(top = 16.dp),
        )

        DictationTextField(
            value = contenido,
            onValueChange = { contenido = it },
            label = "Escribe lo que quieras",
            singleLine = false,
            modifier = Modifier.weight(1f).padding(top = 16.dp),
        )

        if (viewModel.esEdicion) {
            Row(modifier = Modifier.padding(top = 12.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(contenido))
                        Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(end = 8.dp),
                ) { Text("📋 Copiar") }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, contenido)
                        }
                        runCatching { context.startActivity(Intent.createChooser(intent, "Compartir nota")) }
                    },
                    modifier = Modifier.padding(end = 8.dp),
                ) { Text("📤 Compartir") }
                OutlinedButton(onClick = { mostrarConfirmacion = true }) { Text("Eliminar") }
            }
        }

        Button(
            onClick = { viewModel.guardar(titulo, contenido) },
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina esta nota para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminar() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }
}
