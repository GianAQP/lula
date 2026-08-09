package com.aqpseller.lulaapp.features.weekly_review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState

@Composable
fun WeeklyReviewHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: WeeklyReviewHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.cargando && uiState.semanas.isEmpty()) {
        EmptyState(
            emoji = "📜",
            titulo = "Todavía no tienes revisiones semanales guardadas.",
            subtitulo = "Cuando guardes una, va a quedar acá para siempre — nada se borra solo.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Semanas anteriores", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(uiState.semanas, key = { it.semana }) { item ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(text = item.etiqueta, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Cumplimiento: ${item.cumplimientoPorcentaje}% · 🔥 Racha máxima: ${item.rachaMaxima} días",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    item.queLogre?.let { Text(text = "Logré: $it", modifier = Modifier.padding(top = 6.dp)) }
                    item.queNoFunciono?.let { Text(text = "No funcionó: $it", modifier = Modifier.padding(top = 2.dp)) }
                    item.queAjusto?.let { Text(text = "Ajusto: $it", modifier = Modifier.padding(top = 2.dp)) }
                }
                HorizontalDivider()
            }
        }
    }
}
