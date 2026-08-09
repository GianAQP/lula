package com.aqpseller.lulaapp.features.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.cargando && uiState.registros.isEmpty()) {
        EmptyState(
            emoji = "📖",
            titulo = "Todavía no tienes días cerrados.",
            subtitulo = "Cuando cierres tu día, vas a poder verlo acá — poco a poco arma tu historia.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Tu historial", style = MaterialTheme.typography.titleLarge)
        StatPill(
            emoji = "📊",
            valor = "Constancia: ${uiState.constancia}%",
            colorContenedor = LulaRachaContainerLight,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "% de días activos en los últimos 30 días — no se resetea si se rompe una racha.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(uiState.registros, key = { it.id }) { registro ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Row {
                        Text(text = registro.fechaTexto, style = MaterialTheme.typography.titleSmall)
                    }
                    Row(modifier = Modifier.padding(top = 6.dp)) {
                        StatPill(
                            emoji = "⭐",
                            valor = "${registro.puntuacion} pts",
                            colorContenedor = LulaRachaContainerLight,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = "${registro.actividadesCompletadas} de ${registro.actividadesTotales} actividades",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    registro.queLogre?.let { Text(text = "Logré: $it", modifier = Modifier.padding(top = 6.dp)) }
                    registro.queCosto?.let { Text(text = "Costó: $it", modifier = Modifier.padding(top = 2.dp)) }
                    registro.queAjusto?.let { Text(text = "Ajusto: $it", modifier = Modifier.padding(top = 2.dp)) }
                }
                HorizontalDivider()
            }
        }
    }
}
