package com.aqpseller.lulaapp.features.care_circle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aqpseller.lulaapp.domain.model.PermisoCompartir

private fun etiquetaPermiso(permiso: PermisoCompartir): String = when (permiso) {
    PermisoCompartir.PUEDE_VER -> "Puedes ver"
    PermisoCompartir.PUEDE_VER_Y_RECORDAR -> "Puedes ver y recordarle"
}

@Composable
fun LoQueMeComparteScreen(
    modifier: Modifier = Modifier,
    viewModel: LoQueMeComparteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var actividadADejarDeVer by remember { mutableStateOf<ActividadCompartidaUi?>(null) }
    if (uiState.cargando) return

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Lo que me comparten",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (uiState.actividades.isEmpty()) {
            Text(
                text = "Nadie te comparte nada todavía. Cuando alguien te comparta un hábito, " +
                    "tarea, medicamento u otra cosa y aceptes desde \"Mi círculo de cuidado\", va a aparecer acá.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            return
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(uiState.actividades, key = { it.solicitudId }) { item ->
                Card(
                    colors = CardDefaults.cardColors(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${item.emoji} ${item.nombre}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "De ${item.deNombre} — ${etiquetaPermiso(item.permiso)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            text = item.subtitulo,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { actividadADejarDeVer = item }) {
                                Text("Dejar de ver esto")
                            }
                        }
                    }
                }
            }
        }
    }

    actividadADejarDeVer?.let { item ->
        ConfirmarEliminarDialog(
            mensaje = "Vas a dejar de ver \"${item.nombre}\" de ${item.deNombre}. Ella/él puede " +
                "seguir compartiéndolo, pero ya no te va a aparecer acá.",
            onConfirmar = { viewModel.dejarDeVer(item.solicitudId); actividadADejarDeVer = null },
            onCancelar = { actividadADejarDeVer = null },
        )
    }
}
