package com.aqpseller.lulaapp.features.finances

import android.widget.Toast
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.ui.theme.LulaFinanzasContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaHabitoContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight

/**
 * Todo en una sola `LazyColumn` (buscador, resumen, categorías Y la lista de movimientos) — antes
 * el encabezado vivía en una `Column` fija arriba y solo la lista de abajo scrolleaba, así que en
 * celulares de pantalla más chica el encabezado se comía hasta 40% de la pantalla, dejando muy
 * poco espacio real para ver el historial (a pedido del usuario). Al scrollear, el encabezado
 * ahora se va con el resto, dándole toda la pantalla a la lista. El desglose por categoría además
 * arranca colapsado — es lo más largo del encabezado y no siempre hace falta verlo. Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
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
    var mostrarCategorias by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val buscando = uiState.consulta.isNotBlank()
    val hayCategorias = uiState.egresosPorCategoria.isNotEmpty() || uiState.ingresosPorCategoria.isNotEmpty()

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text(text = "Historial", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))

            OutlinedTextField(
                value = uiState.consulta,
                onValueChange = viewModel::buscar,
                label = { Text("🔎 ¿Cuándo gasté/recibí...?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            // Mientras se busca, no tiene sentido navegar por mes/rango — la búsqueda ya
            // recorre todo el historial. Ver `Plan/08-decisiones-tecnicas.md`.
            if (!buscando) {
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
                            // Antes este botón vivía siempre ahí, incluso viendo el mes actual —
                            // debajo de "Agosto 2026" parecía otro dato del mes, no un atajo, y
                            // confundía (a pedido del usuario). Ahora solo aparece cuando de
                            // verdad hay a dónde volver. Ver `08-decisiones-tecnicas.md`.
                            val esMesActual = uiState.mesVisible.year == DateTimeUtils.hoy().year &&
                                uiState.mesVisible.monthNumber == DateTimeUtils.hoy().monthNumber
                            if (!esMesActual) {
                                TextButton(onClick = viewModel::irAHoy) { Text("Ir a hoy") }
                            }
                        }
                        IconButton(onClick = viewModel::mesSiguiente) { Text("▶", style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }

            if (!uiState.cargando) {
                // Sin esto no había forma de saber, de un vistazo, si el período visible cerró
                // en verde o en rojo (a pedido del usuario, ver `Plan/08-decisiones-tecnicas.md`).
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

                // Colapsado por defecto — es lo más largo del encabezado (una fila por
                // categoría) y no siempre hace falta verlo. Sin gráficos a propósito (la app usa
                // texto/emoji, no barras ni tortas). Ver `Plan/08-decisiones-tecnicas.md`.
                if (hayCategorias) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mostrarCategorias = !mostrarCategorias }
                            .padding(top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "📊 Por categoría", style = MaterialTheme.typography.titleSmall)
                        Text(if (mostrarCategorias) "▾ Ocultar" else "▸ Ver", style = MaterialTheme.typography.labelLarge)
                    }
                    if (mostrarCategorias) {
                        if (uiState.egresosPorCategoria.isNotEmpty()) {
                            Text(
                                text = "GASTOS",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            uiState.egresosPorCategoria.forEach { (categoria, monto) ->
                                Text(text = "$categoria — S/ ${"%.2f".format(monto)}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        if (uiState.ingresosPorCategoria.isNotEmpty()) {
                            Text(
                                text = "INGRESOS",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            uiState.ingresosPorCategoria.forEach { (categoria, monto) ->
                                Text(text = "$categoria — S/ ${"%.2f".format(monto)}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (uiState.movimientosVisibles.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(viewModel.textoParaCopiar()))
                            Toast.makeText(context, "Copiado — pégalo en Excel o donde quieras", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(top = if (hayCategorias) 4.dp else 12.dp),
                    ) {
                        Text("📋 Copiar lo que estás viendo")
                    }
                }
            }

            if (!uiState.cargando && uiState.movimientosVisibles.isEmpty()) {
                Text(
                    text = if (buscando) "No encontré nada con \"${uiState.consulta}\"." else "No hay movimientos en este período.",
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            if (uiState.movimientosVisibles.isNotEmpty()) {
                Text(
                    text = "MOVIMIENTOS",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
        }

        items(uiState.movimientosVisibles, key = { it.id }) { movimiento ->
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
