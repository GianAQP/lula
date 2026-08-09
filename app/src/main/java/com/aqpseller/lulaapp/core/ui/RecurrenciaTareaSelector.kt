package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea

fun etiquetaRecurrenciaTarea(recurrencia: RecurrenciaTarea) = when (recurrencia) {
    RecurrenciaTarea.SIN_REPETIR -> "No se repite"
    RecurrenciaTarea.DIARIA -> "Cada día"
    RecurrenciaTarea.SEMANAL -> "Cada semana"
    RecurrenciaTarea.QUINCENAL -> "Cada 15 días"
    RecurrenciaTarea.MENSUAL -> "Cada mes"
    RecurrenciaTarea.BIMESTRAL -> "Cada 2 meses"
    RecurrenciaTarea.TRIMESTRAL -> "Cada 3 meses"
    RecurrenciaTarea.ANUAL -> "Cada año"
}

/**
 * Cada cuánto se repite una Tarea (pagar la luz, agua, etc.) — solo tiene sentido con una fecha
 * límite ya elegida, porque avanza desde esa fecha. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrenciaTareaSelector(
    recurrenciaSeleccionada: RecurrenciaTarea,
    onRecurrenciaSeleccionada: (RecurrenciaTarea) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier) {
        RecurrenciaTarea.entries.forEach { recurrencia ->
            FilterChip(
                selected = recurrenciaSeleccionada == recurrencia,
                onClick = { onRecurrenciaSeleccionada(recurrencia) },
                label = { Text(etiquetaRecurrenciaTarea(recurrencia)) },
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
            )
        }
    }
}
