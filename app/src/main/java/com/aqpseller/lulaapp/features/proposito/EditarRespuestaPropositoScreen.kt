package com.aqpseller.lulaapp.features.proposito

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.domain.model.SeccionProposito

@Composable
fun EditarRespuestaPropositoScreen(
    onGuardado: () -> Unit,
    onEliminada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditarRespuestaPropositoViewModel = hiltViewModel(),
) {
    var respuesta by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    val respuestaInicial by viewModel.respuestaInicial.collectAsState()
    val guardado by viewModel.guardado.collectAsState()
    val eliminada by viewModel.eliminada.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    LaunchedEffect(eliminada) { if (eliminada) onEliminada() }
    LaunchedEffect(respuestaInicial) { respuestaInicial?.let { respuesta = it } }

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text(text = viewModel.pregunta.texto, style = MaterialTheme.typography.titleLarge)
        Text(
            text = if (viewModel.pregunta.seccion == SeccionProposito.PROPOSITO) {
                "El \"para qué\" detrás de todo — no hace falta que sea perfecto, se puede " +
                    "actualizar cuando quieras."
            } else {
                "Ayuda a armar tu Misión y Visión — respondé con lo primero que se te ocurra."
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )

        DictationTextField(
            value = respuesta,
            onValueChange = { respuesta = it },
            label = "Tu respuesta",
            singleLine = false,
            modifier = Modifier.weight(1f).padding(top = 16.dp),
        )

        Row(modifier = Modifier.padding(top = 12.dp)) {
            Button(
                onClick = { viewModel.guardar(respuesta) },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                Text("Guardar")
            }
            if (!respuestaInicial.isNullOrBlank()) {
                OutlinedButton(onClick = { mostrarConfirmacion = true }) {
                    Text("Borrar")
                }
            }
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto borra tu respuesta a \"${viewModel.pregunta.texto}\" para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminar() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }
}
