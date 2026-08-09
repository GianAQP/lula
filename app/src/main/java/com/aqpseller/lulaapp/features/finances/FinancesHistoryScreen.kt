package com.aqpseller.lulaapp.features.finances

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.ui.theme.LulaFinanzasContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaHabitoContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesHistoryScreen(
    onMovimientoClick: (MovimientoUi) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinancesHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarSelectorDesde by remember { mutableStateOf(false) }
    var mostrarSelectorHasta by remember { mutableStateOf(false) }
    var fechaDesdeElegida by remember { mutableStateOf<Long?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Historial", style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.padding(top = 12.dp)) {
            FilterChip(
                selected = !uiState.modoRango,
                onClick = { viewModel.volverAModoMes() },
                label = { Text("Por mes") },
                modifier = Modifier.padding(end = 8.dp),
            )
            FilterChip(
                selected = uiState.modoRango,
                onClick = { mostrarSelectorDesde = true },
                label = { Text("📅 Rango de fechas") },
            )
        }

        if (uiState.modoRango) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = uiState.fechaDesde?.let { DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(it)) } ?: "Desde",
                    modifier = Modifier.clickable { fechaDesdeElegida = uiState.fechaDesde; mostrarSelectorDesde = true },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(text = "→")
                Text(
                    text = uiState.fechaHasta?.let { DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(it)) } ?: "Hasta",
                    modifier = Modifier.clickable {
                        fechaDesdeElegida = uiState.fechaDesde
                        mostrarSelectorHasta = true
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = viewModel::mesAnterior) { Text("◀", style = MaterialTheme.typography.titleLarge) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = DateTimeUtils.formatearMesYAnio(uiState.mesVisible), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::irAHoy) { Text("Hoy") }
                }
                IconButton(onClick = viewModel::mesSiguiente) { Text("▶", style = MaterialTheme.typography.titleLarge) }
            }
        }

        if (!uiState.cargando) {
            // Sin esto no había forma de saber, de un vistazo, si el período visible cerró en
            // verde o en rojo — antes solo se veía la lista de movimientos sueltos (a pedido
            // del usuario, ver `Plan/08-decisiones-tecnicas.md`).
            Row(modifier = Modifier.padding(top = 16.dp)) {
                StatPill(
                    emoji = "📈",
                    valor = "S/ ${"%.0f".format(uiState.totalIngresos)}",
                    colorContenedor = LulaHabitoContainerLight,
                    modifier = Modifier.padding(end = 8.dp),
                )
                StatPill(
                    emoji = "📉",
                    valor = "S/ ${"%.0f".format(uiState.totalEgresos)}",
                    colorContenedor = LulaRachaContainerLight,
                    modifier = Modifier.padding(end = 8.dp),
                )
                StatPill(
                    emoji = "⚖️",
                    valor = "S/ ${"%.0f".format(uiState.balance)}",
                    colorContenedor = LulaFinanzasContainerLight,
                )
            }
        }

        if (!uiState.cargando && uiState.movimientos.isEmpty()) {
            Text(text = "No hay movimientos en este período.", modifier = Modifier.padding(top = 16.dp))
        }

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(uiState.movimientos, key = { it.id }) { movimiento ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMovimientoClick(movimiento) }
                        .padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val signo = if (movimiento.tipo == TipoMovimientoFinanciero.EGRESO) "-" else "+"
                        Text(text = "${movimiento.categoria}  $signo S/ ${"%.2f".format(movimiento.monto)}")
                        if (!movimiento.descripcion.isNullOrBlank()) {
                            Text(text = movimiento.descripcion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = DateTimeUtils.formatearFechaCorta(DateTimeUtils.epochMillisToLocalDate(movimiento.fecha)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = DateTimeUtils.letraDiaSemana(movimiento.fecha),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (mostrarSelectorDesde) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(fechaDesdeElegida ?: DateTimeUtils.inicioDeHoyEpochMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorDesde = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis ->
                        fechaDesdeElegida = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)
                    }
                    mostrarSelectorDesde = false
                    mostrarSelectorHasta = true
                }) { Text("Siguiente") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorDesde = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (mostrarSelectorHasta) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(uiState.fechaHasta ?: DateTimeUtils.inicioDeHoyEpochMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorHasta = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis ->
                        val hasta = DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)
                        val desde = fechaDesdeElegida ?: uiState.fechaDesde ?: hasta
                        viewModel.activarRangoPersonalizado(desde, hasta)
                    }
                    mostrarSelectorHasta = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorHasta = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}
