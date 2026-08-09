package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Fila "ir a otra sección" con ícono a color — más vistosa que un texto subrayado suelto, y
 * el color distingue de qué se trata de un vistazo (mismo lenguaje visual que el bottom nav
 * y el menú `+`). Ver `Plan/09-guia-visual.md`.
 */
@Composable
fun SectionLinkRow(
    emoji: String,
    color: Color,
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColoredEmojiIcon(emoji = emoji, colorContenedor = color, tamano = 32.dp)
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        )
        // Antes usaba el mismo tamaño que el texto de la fila y casi no se notaba — se agranda
        // a propósito para que sea obvio que la fila entera es clickeable.
        Text(
            text = "→",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
