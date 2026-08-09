package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Texto oscuro fijo: los contenedores de StatPill son siempre pasteles claros, en modo claro
 * u oscuro — si el texto heredara el color de tema (blanco en modo oscuro) quedaría invisible
 * sobre el fondo claro. */
private val ColorTextoPill = Color(0xFF2B2B2B)

/**
 * Insignia pequeña tipo "cápsula" (emoji + valor) — mismo patrón que la fila de stats de
 * Duolingo/Me+ (racha, puntos...), pero siempre en tono positivo: nunca rojo de alerta.
 * Ver `Plan/09-guia-visual.md`.
 */
@Composable
fun StatPill(
    emoji: String,
    valor: String,
    colorContenedor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$emoji $valor",
        style = MaterialTheme.typography.labelLarge,
        color = ColorTextoPill,
        modifier = modifier
            .background(color = colorContenedor, shape = RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
