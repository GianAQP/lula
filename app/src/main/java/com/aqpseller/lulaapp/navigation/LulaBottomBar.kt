package com.aqpseller.lulaapp.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aqpseller.lulaapp.core.ui.ColoredEmojiIcon
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerLight

/**
 * Bottom bar de 5 posiciones (Hoy fija + 3 configurables + `+` central) con los valores por
 * defecto de `Plan/02-pantallas.md`. Cada posición tiene su propio color de marca (ver
 * `Plan/09-guia-visual.md`), igual que en Duolingo/Me+, para distinguir secciones de un
 * vistazo sin leer la etiqueta. Las posiciones 2-4 se personalizan desde Ajustes
 * (`OpcionBottomBar`, `AjustesRepository`) — acá solo se resuelve el id guardado a su
 * emoji/color/ruta. Los 3 ítems configurables se escriben en línea (no en una función
 * helper aparte) porque una función `@Composable` privada separada, con estos mismos
 * argumentos, hacía fallar la resolución de `NavigationBarItem` con el compilador de Compose
 * de este proyecto — en línea, exactamente igual que Hoy/`+`, compila sin problema.
 */
@Composable
fun LulaBottomBar(
    currentRoute: String?,
    posicion2Id: String,
    posicion3Id: String,
    posicion4Id: String,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    val opcion2 = OpcionBottomBar.porId(posicion2Id)
    val opcion3 = OpcionBottomBar.porId(posicion3Id)
    val opcion4 = OpcionBottomBar.porId(posicion4Id)
    val seleccionado2 = opcion2.estaSeleccionada(currentRoute)
    val seleccionado3 = opcion3.estaSeleccionada(currentRoute)
    val seleccionado4 = opcion4.estaSeleccionada(currentRoute)

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == LulaDestinations.HOY,
            onClick = { onNavigate(LulaDestinations.HOY) },
            icon = { IconoNavColoreado("🏠", LulaPrimaryContainerLight, currentRoute == LulaDestinations.HOY) },
            label = { Text("Hoy") },
            colors = coloresSinIndicador(),
        )
        NavigationBarItem(
            selected = seleccionado2,
            onClick = { onNavigate(opcion2.rutaDeNavegacion()) },
            icon = { IconoNavColoreado(opcion2.emoji, opcion2.color(), seleccionado2) },
            label = { Text(opcion2.etiqueta) },
            colors = coloresSinIndicador(),
        )
        NavigationBarItem(
            selected = false,
            onClick = onAddClick,
            icon = { IconoNavColoreado("➕", LulaPrimaryContainerLight, false) },
            label = { Text("Agregar") },
            colors = coloresSinIndicador(),
        )
        NavigationBarItem(
            selected = seleccionado3,
            onClick = { onNavigate(opcion3.rutaDeNavegacion()) },
            icon = { IconoNavColoreado(opcion3.emoji, opcion3.color(), seleccionado3) },
            label = { Text(opcion3.etiqueta) },
            colors = coloresSinIndicador(),
        )
        NavigationBarItem(
            selected = seleccionado4,
            onClick = { onNavigate(opcion4.rutaDeNavegacion()) },
            icon = { IconoNavColoreado(opcion4.emoji, opcion4.color(), seleccionado4) },
            label = { Text(opcion4.etiqueta) },
            colors = coloresSinIndicador(),
        )
    }
}

/** El círculo de color ya marca la selección — se desactiva el indicador default de Material3. */
@Composable
private fun coloresSinIndicador() = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)

@Composable
private fun IconoNavColoreado(emoji: String, colorSeleccionado: Color, seleccionado: Boolean) {
    ColoredEmojiIcon(
        emoji = emoji,
        colorContenedor = if (seleccionado) colorSeleccionado else Color.Transparent,
        tamano = 44.dp,
    )
}
