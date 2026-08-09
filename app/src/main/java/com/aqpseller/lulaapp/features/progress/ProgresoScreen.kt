package com.aqpseller.lulaapp.features.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.ui.theme.LulaHabitoContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight

/** "Tu semana" de `02-pantallas.md` — resumen + entrada a la Revisión semanal completa. */
@Composable
fun ProgresoScreen(
    onVerRevisionSemanal: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgresoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Tu progreso", style = MaterialTheme.typography.titleLarge)

        if (uiState.cargando) return@Column

        Text(text = "Tu semana", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            StatPill(
                emoji = "📈",
                valor = "Cumplimiento: ${uiState.cumplimientoSemana}%",
                colorContenedor = LulaHabitoContainerLight,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            StatPill(
                emoji = "🔥",
                valor = "Racha máxima: ${uiState.rachaMaximaSemana} días",
                colorContenedor = LulaRachaContainerLight,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            StatPill(
                emoji = "📊",
                valor = "Constancia (30 días): ${uiState.constancia30Dias}%",
                colorContenedor = LulaRachaContainerLight,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Text(
            text = "Puntos esta semana: ${uiState.puntosSemana}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        Button(onClick = onVerRevisionSemanal, modifier = Modifier.padding(top = 24.dp)) {
            Text("Revisión semanal completa →")
        }
    }
}
