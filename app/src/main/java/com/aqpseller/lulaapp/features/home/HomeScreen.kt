package com.aqpseller.lulaapp.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.domain.model.EstadoActividad

@Composable
fun HomeScreen(
    onCerrarDia: () -> Unit,
    onAgregarAlgo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.cargando) {
            return@Column
        }

        if (!uiState.hayAlgoParaHoy) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Todavía no tienes actividades para hoy.")
                Button(onClick = onAgregarAlgo, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Agregar algo para hoy")
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text(text = "🔥 Racha: ${uiState.racha} días", style = MaterialTheme.typography.titleMedium)
                val progreso = if (uiState.totalActividades > 0) {
                    uiState.completadas.toFloat() / uiState.totalActividades
                } else {
                    0f
                }
                Text(text = "Progreso de hoy")
                LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
                Text(text = "${uiState.completadas} de ${uiState.totalActividades}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            seccionActividades("MAÑANA", uiState.actividadesManana, viewModel)
            seccionActividades("TARDE", uiState.actividadesTarde, viewModel)
            seccionActividades("NOCHE", uiState.actividadesNoche, viewModel)
            seccionActividades("TAREAS DE HOY", uiState.tareasDeHoy, viewModel)

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(text = "Resumen rápido", style = MaterialTheme.typography.titleSmall)
                Text(text = "💰 Gastos hoy: S/ ${"%.2f".format(uiState.gastosHoyTotal)}")
            }
        }

        Button(
            onClick = onCerrarDia,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(if (uiState.diaYaCerrado) "Actualizar cierre del día" else "Cerrar mi día")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.seccionActividades(
    titulo: String,
    actividades: List<ActividadUi>,
    viewModel: HomeViewModel,
) {
    if (actividades.isEmpty()) return
    item {
        Text(text = titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    }
    items(actividades, key = { it.id }) { actividad ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = actividad.estado == EstadoActividad.CONFIRMADO,
                onCheckedChange = { marcado ->
                    viewModel.marcarActividad(
                        actividad.id,
                        if (marcado) EstadoActividad.CONFIRMADO else EstadoActividad.SIN_CONFIRMAR,
                    )
                },
            )
            Text(text = actividad.nombre)
        }
    }
}
