package com.aqpseller.lulaapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Los colores de marca "XxxContainerLight"/"XxxContainerDark" no son roles reconocidos por
 * `MaterialTheme.colorScheme` (son constantes fijas de `Color.kt`, no varían solas con el
 * tema) — un `Card`/chip con `containerColor` fijado a la variante "Light" en modo oscuro
 * terminaba con el texto claro que hereda del tema sobre un fondo también claro, casi
 * invisible ("un cuadro medio blanco donde no se nota el texto"). Este helper resuelve el par
 * contenedor+contenido correcto según el tema del sistema — usar en cualquier superficie que
 * use un color de marca y contenga texto.
 */
@Composable
fun lulaCardColors(claro: Color, oscuro: Color): CardColors {
    val enOscuro = isSystemInDarkTheme()
    return CardDefaults.cardColors(
        containerColor = if (enOscuro) oscuro else claro,
        contentColor = if (enOscuro) Color.White else Color.Black,
    )
}

@Composable
fun lulaContainerColor(claro: Color, oscuro: Color): Color = if (isSystemInDarkTheme()) oscuro else claro

@Composable
fun lulaContentColorSobreContainer(): Color = if (isSystemInDarkTheme()) Color.White else Color.Black
