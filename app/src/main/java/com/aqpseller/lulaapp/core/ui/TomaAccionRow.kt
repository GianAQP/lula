package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.SonidoUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.ui.theme.LulaHabito

/**
 * Fila de una toma de Medicamento: hora + instrucción original (ej. "Después del almuerzo") y
 * los 3 estados posibles — pendiente/tomada/omitida, todos igual de válidos (nunca un castigo
 * por omitir, ver `Plan/CLAUDE.md`). "Tomada" usa un `Checkbox` real (vacío hasta que se marca,
 * igual que el resto de la app) — antes el texto "✅ Tomada" ya traía el visto puesto en la
 * etiqueta incluso pendiente, y parecía ya marcado sin estarlo. Reusada por Hoy, "Mi salud" y
 * `AccionTomaScreen`.
 */
@Composable
fun TomaAccionRow(
    nombreMedicamento: String,
    horario: String,
    instruccion: String,
    estado: EstadoActividad,
    onMarcar: (EstadoActividad) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sonidoCheckViewModel: SonidoCheckViewModel = hiltViewModel()
    val sonidoHabilitado by sonidoCheckViewModel.habilitado.collectAsState()

    // Ya pasó la hora de esta toma y sigue sin marcarse — se resalta para que llame la atención
    // (a pedido del usuario, mismo criterio que ya se usa en Citas/Fechas importantes de Hoy).
    val horaActual = DateTimeUtils.horaHHmm(DateTimeUtils.ahoraEpochMillis())
    val vencida = estado == EstadoActividad.SIN_CONFIRMAR && horario < horaActual
    val colorTexto = if (vencida) MaterialTheme.colorScheme.error else Color.Unspecified

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (vencida) Modifier.background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = estado == EstadoActividad.CONFIRMADO,
                onCheckedChange = { marcado ->
                    onMarcar(if (marcado) EstadoActividad.CONFIRMADO else EstadoActividad.SIN_CONFIRMAR)
                    if (sonidoHabilitado) SonidoUtils.reproducirCheck()
                },
                colors = CheckboxDefaults.colors(checkedColor = LulaHabito, checkmarkColor = Color.White),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💊 $horario — $nombreMedicamento" + if (vencida) " ⚠️" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorTexto,
                    textDecoration = if (estado == EstadoActividad.CONFIRMADO) TextDecoration.LineThrough else null,
                )
                Text(text = instruccion, style = MaterialTheme.typography.bodySmall, color = colorTexto)
            }
        }
        Row(modifier = Modifier.padding(start = 40.dp)) {
            if (estado == EstadoActividad.OMITIDO) {
                AssistChip(
                    onClick = { onMarcar(EstadoActividad.SIN_CONFIRMAR) },
                    label = { Text("⏭️ Omitida — toca para deshacer") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                TextButton(onClick = { onMarcar(EstadoActividad.OMITIDO) }) { Text("⏭️ Omito") }
            }
        }
    }
}
