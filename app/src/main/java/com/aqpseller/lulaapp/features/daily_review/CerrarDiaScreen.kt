package com.aqpseller.lulaapp.features.daily_review

import androidx.compose.foundation.layout.Column
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
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight

@Composable
fun CerrarDiaScreen(
    onVolverAHoy: () -> Unit,
    onVerHistorial: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CerrarDiaViewModel = hiltViewModel(),
) {
    var queLogre by remember { mutableStateOf("") }
    var queCosto by remember { mutableStateOf("") }
    var queAjusto by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    // Se dispara una sola vez, cuando termina de cargar — si hoy ya se había cerrado antes, esto
    // trae las respuestas existentes en vez de dejar los campos en blanco (ver ViewModel).
    LaunchedEffect(uiState.cargando) {
        if (!uiState.cargando) {
            queLogre = uiState.queLogreInicial.orEmpty()
            queCosto = uiState.queCostoInicial.orEmpty()
            queAjusto = uiState.queAjustoInicial.orEmpty()
        }
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        if (uiState.cerrado) {
            Text(text = "Buen trabajo. Mañana seguimos.", style = MaterialTheme.typography.titleMedium)
            StatPill(
                emoji = "🔥",
                valor = "Racha: ${uiState.rachaFinal} días",
                colorContenedor = LulaRachaContainerLight,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = onVolverAHoy, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Volver a Hoy")
                }
                OutlinedButton(onClick = onVerHistorial) {
                    Text("Ver mi historial")
                }
            }
            return
        }

        Text(
            text = if (uiState.yaExistiaRegistro) "Actualiza el cierre de hoy" else "Cerremos tu día",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Actividades: ${uiState.actividadesCompletadas} de ${uiState.actividadesTotales}",
            modifier = Modifier.padding(top = 8.dp),
        )

        DictationTextField(
            value = queLogre,
            onValueChange = { queLogre = it },
            label = "¿Qué logré hoy? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = queCosto,
            onValueChange = { queCosto = it },
            label = "¿Qué costó más? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = queAjusto,
            onValueChange = { queAjusto = it },
            label = "¿Qué ajusto para mañana? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            text = "Puntuación del día: ${uiState.actividadesCompletadas} puntos",
            modifier = Modifier.padding(top = 16.dp),
        )

        Button(
            onClick = {
                viewModel.cerrarDia(
                    queLogre.ifBlank { null },
                    queCosto.ifBlank { null },
                    queAjusto.ifBlank { null },
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (uiState.yaExistiaRegistro) "Guardar cambios" else "Guardar y cerrar")
        }
    }
}
