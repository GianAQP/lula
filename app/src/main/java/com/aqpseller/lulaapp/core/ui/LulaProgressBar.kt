package com.aqpseller.lulaapp.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Barra de progreso tipo "píldora" gruesa, con el color de marca — reemplaza la
 * `LinearProgressIndicator` delgada por defecto de Material3. Ver `Plan/09-guia-visual.md`.
 */
@Composable
fun LulaProgressBar(
    progreso: Float,
    modifier: Modifier = Modifier,
) {
    val progresoAnimado by animateFloatAsState(targetValue = progreso.coerceIn(0f, 1f), label = "progreso")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progresoAnimado)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
