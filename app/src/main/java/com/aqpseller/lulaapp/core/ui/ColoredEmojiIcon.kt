package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Emoji dentro de un círculo de color — usado en el bottom nav y en el menú `+` para que cada
 * opción se reconozca por color, no solo por texto. Ver `Plan/09-guia-visual.md`.
 */
@Composable
fun ColoredEmojiIcon(
    emoji: String,
    colorContenedor: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(tamano)
            .background(color = colorContenedor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = (tamano.value * 0.5).sp)
    }
}
