package com.aqpseller.lulaapp.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Bottom bar de 5 posiciones (Hoy fija + 3 configurables + `+` central) con los valores por
 * defecto de `Plan/02-pantallas.md`. La personalización de posiciones 2-4 queda para una
 * sesión futura; por ahora usan siempre el default.
 */
@Composable
fun LulaBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == LulaDestinations.HOY,
            onClick = { onNavigate(LulaDestinations.HOY) },
            icon = { Text("🏠") },
            label = { Text("Hoy") },
        )
        NavigationBarItem(
            selected = currentRoute == LulaDestinations.proximamente("Asistente"),
            onClick = { onNavigate(LulaDestinations.proximamente("Asistente")) },
            icon = { Text("🎙️") },
            label = { Text("Asistente") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onAddClick,
            icon = { Text("➕") },
            label = { Text("Agregar") },
        )
        NavigationBarItem(
            selected = currentRoute == LulaDestinations.proximamente("Hábitos"),
            onClick = { onNavigate(LulaDestinations.proximamente("Hábitos")) },
            icon = { Text("✅") },
            label = { Text("Hábitos") },
        )
        NavigationBarItem(
            selected = currentRoute == LulaDestinations.proximamente("Finanzas"),
            onClick = { onNavigate(LulaDestinations.proximamente("Finanzas")) },
            icon = { Text("💰") },
            label = { Text("Finanzas") },
        )
    }
}
