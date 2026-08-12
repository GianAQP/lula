package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

/**
 * Atajos rápidos de fecha (1 semana / 1 mes / 3 meses / 1 año, siempre contados desde hoy) más
 * el calendario de siempre para una fecha puntual — evita obligar a abrir el selector completo
 * para el caso común de "más o menos en un mes". Reutilizado por Crear/Editar Meta y por
 * "🔜 Aplazar" — pedido explícito del usuario. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectorFechaRapida(
    fechaActual: Long?,
    onFechaElegida: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    fun fechaEnMillis(periodo: DatePeriod): Long =
        DateTimeUtils.localDateAEpochMillis(DateTimeUtils.hoy().plus(periodo))

    FlowRow(modifier = modifier) {
        FilterChip(
            selected = false,
            onClick = { onFechaElegida(fechaEnMillis(DatePeriod(days = 7))) },
            label = { Text("+1 semana") },
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
        )
        FilterChip(
            selected = false,
            onClick = { onFechaElegida(fechaEnMillis(DatePeriod(months = 1))) },
            label = { Text("+1 mes") },
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
        )
        FilterChip(
            selected = false,
            onClick = { onFechaElegida(fechaEnMillis(DatePeriod(months = 3))) },
            label = { Text("+3 meses") },
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
        )
        FilterChip(
            selected = false,
            onClick = { onFechaElegida(fechaEnMillis(DatePeriod(years = 1))) },
            label = { Text("+1 año") },
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
        )
        FilterChip(
            selected = false,
            onClick = { mostrarCalendario = true },
            label = { Text("📅 Elegir fecha") },
            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
        )
    }

    if (mostrarCalendario) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(fechaActual ?: DateTimeUtils.inicioDeHoyEpochMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis -> onFechaElegida(DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)) }
                    mostrarCalendario = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estadoFecha) }
    }
}
