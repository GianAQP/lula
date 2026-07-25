package com.aqpseller.lulaapp.features.daily_review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun CerrarDiaScreen(
    onVolverAHoy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CerrarDiaViewModel = hiltViewModel(),
) {
    var queLogre by remember { mutableStateOf("") }
    var queCosto by remember { mutableStateOf("") }
    var queAjusto by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.padding(16.dp)) {
        if (uiState.cerrado) {
            Text(
                text = "Buen trabajo. Mañana seguimos. 🔥 Racha: ${uiState.rachaFinal} días",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onVolverAHoy, modifier = Modifier.padding(top = 16.dp)) {
                Text("Volver a Hoy")
            }
            return
        }

        Text(text = "Cerremos tu día", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Actividades: ${uiState.actividadesCompletadas} de ${uiState.actividadesTotales}",
            modifier = Modifier.padding(top = 8.dp),
        )

        OutlinedTextField(
            value = queLogre,
            onValueChange = { queLogre = it },
            label = { Text("¿Qué logré hoy? (opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        OutlinedTextField(
            value = queCosto,
            onValueChange = { queCosto = it },
            label = { Text("¿Qué costó más? (opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        OutlinedTextField(
            value = queAjusto,
            onValueChange = { queAjusto = it },
            label = { Text("¿Qué ajusto para mañana? (opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
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
            Text("Guardar y cerrar")
        }
    }
}
