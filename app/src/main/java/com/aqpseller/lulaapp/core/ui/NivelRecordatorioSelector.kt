package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio

private fun etiqueta(nivel: NivelRecordatorio) = when (nivel) {
    NivelRecordatorio.SILENCIOSO -> "🔇 Silencioso"
    NivelRecordatorio.SONIDO -> "🔔 Sonido"
    NivelRecordatorio.ALARMA -> "⏰ Alarma"
}

/**
 * Qué tan insistente es el recordatorio — elegido por el usuario, nunca por Lula. Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
@Composable
fun NivelRecordatorioSelector(
    nivelSeleccionado: NivelRecordatorio,
    onNivelSeleccionado: (NivelRecordatorio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        NivelRecordatorio.entries.forEach { nivel ->
            FilterChip(
                selected = nivelSeleccionado == nivel,
                onClick = { onNivelSeleccionado(nivel) },
                label = { Text(etiqueta(nivel)) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
