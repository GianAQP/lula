package com.aqpseller.lulaapp.navigation

import androidx.compose.ui.graphics.Color
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaFinanzasContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaHabitoContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaTareaContainerLight

/**
 * Las 7 opciones para las posiciones 2-4 de la barra inferior, tal como las enumera
 * `Plan/02-pantallas.md`. `id` es lo único que se persiste (`AjustesRepository`) — dominio no
 * conoce rutas de navegación, así que la ruta/color viven acá, en la capa de navegación.
 */
enum class OpcionBottomBar(val id: String, val emoji: String, val etiqueta: String) {
    ASISTENTE("asistente", "🎙️", "Asistente"),
    HABITOS("habitos", "✅", "Hábitos"),
    PROGRESO("progreso", "📊", "Progreso"),
    FINANZAS("finanzas", "💰", "Finanzas"),
    TAREAS("tareas", "📅", "Tareas"),
    METAS("metas", "🎯", "Metas"),
    CIRCULO_CUIDADO("circulo_cuidado", "👥", "Círculo de cuidado");

    companion object {
        fun porId(id: String): OpcionBottomBar = entries.find { it.id == id } ?: ASISTENTE
    }
}

fun OpcionBottomBar.rutaDeNavegacion(): String = when (this) {
    OpcionBottomBar.ASISTENTE -> LulaDestinations.proximamente("Asistente")
    OpcionBottomBar.HABITOS -> LulaDestinations.HABITOS
    OpcionBottomBar.PROGRESO -> LulaDestinations.PROGRESO
    // Finanzas siempre entra por el gate de Zona Privada, nunca directo.
    OpcionBottomBar.FINANZAS -> LulaDestinations.ZONA_PRIVADA_GATE
    OpcionBottomBar.TAREAS -> LulaDestinations.TAREAS
    OpcionBottomBar.METAS -> LulaDestinations.METAS
    OpcionBottomBar.CIRCULO_CUIDADO -> LulaDestinations.CIRCULO_CUIDADO
}

fun OpcionBottomBar.color(): Color = when (this) {
    OpcionBottomBar.ASISTENTE -> LulaAsistenteContainerLight
    OpcionBottomBar.HABITOS -> LulaHabitoContainerLight
    OpcionBottomBar.PROGRESO -> LulaPrimaryContainerLight
    OpcionBottomBar.FINANZAS -> LulaFinanzasContainerLight
    OpcionBottomBar.TAREAS -> LulaTareaContainerLight
    OpcionBottomBar.METAS -> LulaPrimaryContainerLight
    OpcionBottomBar.CIRCULO_CUIDADO -> LulaPrimaryContainerLight
}

/** Finanzas se navega vía el gate, pero también cuenta como "seleccionada" si ya se pasó y se está en la pantalla real. */
fun OpcionBottomBar.estaSeleccionada(currentRoute: String?): Boolean = when (this) {
    OpcionBottomBar.FINANZAS -> currentRoute == LulaDestinations.FINANZAS || currentRoute == LulaDestinations.ZONA_PRIVADA_GATE
    else -> currentRoute == rutaDeNavegacion()
}
