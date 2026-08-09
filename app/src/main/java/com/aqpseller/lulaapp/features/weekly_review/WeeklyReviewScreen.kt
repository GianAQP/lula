package com.aqpseller.lulaapp.features.weekly_review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.LulaProgressBar

@Composable
fun WeeklyReviewScreen(
    onVerHistorial: () -> Unit,
    onGuardada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyReviewViewModel = hiltViewModel(),
) {
    var queLogre by remember { mutableStateOf("") }
    var queNoFunciono by remember { mutableStateOf("") }
    var queAjusto by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.guardadoExitoso) { if (uiState.guardadoExitoso) onGuardada() }
    LaunchedEffect(uiState.cargando) {
        if (!uiState.cargando) {
            queLogre = uiState.queLogreGuardado
            queNoFunciono = uiState.queNoFuncionoGuardado
            queAjusto = uiState.queAjustoGuardado
        }
    }
    if (uiState.cargando) return

    if (!uiState.activada) {
        Column(modifier = modifier.fillMaxSize()) {
            EmptyState(
                emoji = "🗓️",
                titulo = "Tu revisión semanal se activa los ${uiState.nombreDiaActivacion}.",
                subtitulo = "Todavía es muy pronto para ver cómo terminó la semana — vuelve ese día.",
                textoBoton = "Ver semanas anteriores",
                onBotonClick = onVerHistorial,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Tu semana", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onVerHistorial, modifier = Modifier.padding(top = 4.dp)) {
            Text("📜 Ver semanas anteriores")
        }

        Text(text = "Cumplimiento general: ${uiState.cumplimientoPorcentaje}%", modifier = Modifier.padding(top = 16.dp))
        LulaProgressBar(progreso = uiState.cumplimientoPorcentaje / 100f, modifier = Modifier.padding(top = 8.dp))
        Text(text = "🔥 Racha máxima de la semana: ${uiState.rachaMaxima} días", modifier = Modifier.padding(top = 12.dp))

        uiState.mejorHabito?.let {
            Text(
                text = "Lo que mejor funcionó\n✓ ${it.nombre} — ${it.porcentaje}%",
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        uiState.peorHabito?.let {
            Text(
                text = "Lo que costó más\n✗ ${it.nombre} — ${it.porcentaje}%",
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (uiState.guardada) {
            Text(
                text = "Ya tienes una revisión guardada esta semana — la ves abajo, y puedes " +
                    "modificarla o completarla antes de actualizarla.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        DictationTextField(
            value = queLogre,
            onValueChange = { queLogre = it },
            label = "¿Qué logré esta semana? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = queNoFunciono,
            onValueChange = { queNoFunciono = it },
            label = "¿Qué no funcionó? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = queAjusto,
            onValueChange = { queAjusto = it },
            label = "¿Qué ajusto la próxima semana? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )

        Button(
            onClick = {
                viewModel.guardarRevision(
                    queLogre.ifBlank { null },
                    queNoFunciono.ifBlank { null },
                    queAjusto.ifBlank { null },
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (uiState.guardada) "Actualizar revisión" else "Guardar revisión")
        }
    }
}
