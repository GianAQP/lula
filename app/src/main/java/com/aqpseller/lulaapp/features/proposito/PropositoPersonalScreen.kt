package com.aqpseller.lulaapp.features.proposito

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.aqpseller.lulaapp.core.ui.LulaProgressBar
import com.aqpseller.lulaapp.domain.model.SeccionProposito

@Composable
fun PropositoPersonalScreen(
    onPreguntaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PropositoPersonalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    if (uiState.cargando) return

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "🧭 Mi propósito", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Se arma de a poco, respondiendo lo que quieras cuando quieras — no hace " +
                "falta terminarlo de una vez. Después lo puedes leer, actualizar o borrar " +
                "cuando quieras.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "${uiState.respondidas} de ${uiState.total} preguntas respondidas",
            modifier = Modifier.padding(top = 12.dp),
        )
        LulaProgressBar(
            progreso = if (uiState.total > 0) uiState.respondidas.toFloat() / uiState.total else 0f,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (uiState.respondidas > 0) {
            OutlinedButton(onClick = { mostrarConfirmacion = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("🗑️ Borrar mi propósito")
            }
        }

        SeccionPreguntas(
            titulo = "🧭 Mi misión y visión",
            subtitulo = "Preguntas de autoconocimiento — quién soy, qué quiero.",
            preguntas = uiState.preguntas.filter { it.seccion == SeccionProposito.MISION_VISION },
            onPreguntaClick = onPreguntaClick,
        )
        SeccionPreguntas(
            titulo = "🎯 Mi propósito",
            subtitulo = "El \"para qué\" detrás de todo lo anterior.",
            preguntas = uiState.preguntas.filter { it.seccion == SeccionProposito.PROPOSITO },
            onPreguntaClick = onPreguntaClick,
        )

        Text(
            text = "Cuando tengas suficientes respuestas, este botón va a mandarlas a armar y " +
                "presentar tu Misión, Visión y Propósito ya redactados.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Button(onClick = {}, enabled = false, modifier = Modifier.padding(top = 8.dp)) {
            Text("🤖 Armar y presentar con IA (próximamente)")
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto borra las ${uiState.respondidas} respuestas que ya escribiste, para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminarTodo() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }
}

@Composable
private fun SeccionPreguntas(
    titulo: String,
    subtitulo: String,
    preguntas: List<PreguntaPropositoUi>,
    onPreguntaClick: (String) -> Unit,
) {
    Text(text = titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 24.dp))
    Text(text = subtitulo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    preguntas.forEach { pregunta ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPreguntaClick(pregunta.id) }
                .padding(vertical = 10.dp),
        ) {
            Text(text = pregunta.texto, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pregunta.respuesta?.takeIf { it.isNotBlank() } ?: "Sin responder — toca para escribir",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        HorizontalDivider()
    }
}
