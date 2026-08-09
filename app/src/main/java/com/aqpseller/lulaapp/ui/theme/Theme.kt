package com.aqpseller.lulaapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LulaPrimaryLight,
    onPrimary = LulaFondoOscuro,
    primaryContainer = LulaPrimaryContainerDark,
    secondary = LulaHabito,
    tertiary = LulaAsistente,
    background = LulaFondoOscuro,
    surface = LulaSuperficieOscuro,
)

private val LightColorScheme = lightColorScheme(
    primary = LulaPrimary,
    onPrimary = LulaSuperficieClaro,
    primaryContainer = LulaPrimaryContainerLight,
    secondary = LulaHabito,
    tertiary = LulaAsistente,
    background = LulaFondoClaro,
    surface = LulaSuperficieClaro,
)

/**
 * Sin dynamic color: Lula tiene su propia identidad visual (cálida, positiva, ver
 * `Plan/09-guia-visual.md`) y no debe variar según el fondo de pantalla del teléfono.
 */
@Composable
fun LulaAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
