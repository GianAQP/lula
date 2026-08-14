package com.aqpseller.lulaapp.features.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.SectionLinkRow
import com.aqpseller.lulaapp.core.ui.TomaAccionRow
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerDark
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerLight
import com.aqpseller.lulaapp.ui.theme.lulaCardColors

@Composable
fun HealthScreen(
    onNuevoMedicamento: () -> Unit,
    onNuevaCita: () -> Unit,
    onVerMedicamento: (String) -> Unit,
    onVerCita: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.cargando) return

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Mi salud",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        if (!uiState.hayAlgo) {
            EmptyState(
                emoji = "💊",
                titulo = "Todavía no tienes medicamentos ni citas registradas.",
                textoBoton = "Agregar medicamento",
                onBotonClick = onNuevoMedicamento,
                modifier = Modifier.weight(1f),
            )
            return@Column
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                SectionLinkRow(emoji = "💊", color = LulaAsistenteContainerLight, texto = "Nuevo medicamento", onClick = onNuevoMedicamento)
                SectionLinkRow(emoji = "🩺", color = LulaPrimaryContainerLight, texto = "Nueva cita", onClick = onNuevaCita)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            if (uiState.medicamentos.isNotEmpty()) {
                item {
                    Text(text = "MEDICAMENTOS", style = MaterialTheme.typography.titleSmall)
                }
                items(uiState.medicamentos, key = { it.actividadId }) { medicamento ->
                    Card(
                        colors = lulaCardColors(LulaAsistenteContainerLight, LulaAsistenteContainerDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVerMedicamento(medicamento.actividadId) },
                            ) {
                                Text(text = "${medicamento.nombre} — ${medicamento.dosis}", style = MaterialTheme.typography.bodyLarge)
                            }
                            if (medicamento.tomasHoy.isEmpty()) {
                                Text(
                                    text = "Sin tomas programadas hoy",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            } else {
                                medicamento.tomasHoy.forEach { toma ->
                                    TomaAccionRow(
                                        nombreMedicamento = medicamento.nombre,
                                        horario = toma.horario,
                                        instruccion = toma.instruccion,
                                        estado = toma.estado,
                                        onMarcar = { nuevoEstado -> viewModel.marcarToma(medicamento.actividadId, toma.horario, nuevoEstado) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.proximasCitas.isNotEmpty()) {
                item {
                    Text(text = "PRÓXIMAS CITAS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                }
                val ahora = DateTimeUtils.ahoraEpochMillis()
                items(uiState.proximasCitas, key = { it.actividadId }) { cita ->
                    // Una sesión de curso vencida sin marcar se resalta igual que en Hoy/Calendario.
                    val vencida = cita.esCurso && cita.fechaHora < ahora
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVerCita(cita.actividadId) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            text = cita.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (vencida) MaterialTheme.colorScheme.error else Color.Unspecified,
                        )
                        Text(
                            text = "📅 ${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(cita.fechaHora))} · ${DateTimeUtils.horaHHmm(cita.fechaHora)}" +
                                (cita.lugar?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (vencida) MaterialTheme.colorScheme.error else Color.Unspecified,
                        )
                        cita.progresoTexto?.let {
                            Text(text = "🔁 $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
