package com.aqpseller.lulaapp.core.utils

/**
 * Textos de celebración al cerrar el día — varios por caso, elegidos al azar, para que no se
 * sienta repetitivo con el uso diario. Tono cálido y breve, coherente con el resto de la app
 * (nunca "coach de gimnasio"). A pedido del usuario. Ver `Plan/08-decisiones-tecnicas.md`.
 */
private val MENSAJES_CIERRE_DIARIO = listOf(
    "Hoy cumpliste contigo. ✅",
    "Un día más sumando.",
    "Hoy avanzaste. Mañana seguimos.",
    "Lo hiciste hoy. Eso cuenta.",
    "Así se construye la constancia. 🔥",
    "Otro día a tu favor.",
    "Hoy elegiste seguir. Bien ahí.",
    "Pequeño paso, pero fue tuyo.",
    "Mañana Lula te espera de nuevo. 🌱",
    "Tu yo de mañana te lo va a agradecer.",
    "Ya casi eres una persona de rutina. Casi. 😄",
    "Lula está orgullosa. No lo dice muy seguido.",
)

private val MENSAJES_HITO_7 = listOf(
    "7 días. Una semana cumpliendo contigo. 🌱",
    "Primera semana lograda — esto recién empieza.",
    "7 días de constancia. Vas bien.",
    "Una semana entera. Pequeñas acciones, grandes cambios.",
    "7 días seguidos. Ya no es casualidad.",
)

private val MENSAJES_HITO_21 = listOf(
    "21 días. Ya no estás empezando, estás avanzando. 🌿",
    "Tres semanas cumpliendo contigo.",
    "21 días. Esto ya se está volviendo parte de ti.",
    "21 días de decisiones chicas construyendo algo grande.",
    "Tres semanas después, ya eres otra persona con esto.",
)

private val MENSAJES_HITO_30 = listOf(
    "30 días. Mira lo que puedes construir siendo constante. 🌳",
    "Un mes completo. No llegaste hasta aquí por suerte.",
    "30 días. Esto ya tiene raíces.",
    "Un mes de constancia. Esto recién empieza.",
    "30 días seguidos. Eso ya es un hábito de verdad.",
)

private fun mensajesHitoMultiplo(dias: Int) = listOf(
    "$dias días seguidos. Constancia real. 🌳",
    "$dias días. Esto ya es parte de tu historia.",
    "$dias días construyendo, sin pausas.",
    "$dias días seguidos. Impresionante.",
)

private val MENSAJES_ANTICIPACION: List<(Int) -> String> = listOf(
    { dias -> "Mañana completas $dias días. Ya casi. 🌱" },
    { dias -> "Un día más y llegas a $dias. Vamos." },
    { dias -> "$dias días mañana, si sigues así." },
)

/** true si [racha] es un hito a celebrar — 7, 21, 30, y cada 30 días después de ese. */
fun esHitoRacha(racha: Int): Boolean = racha == 7 || racha == 21 || (racha >= 30 && racha % 30 == 0)

/** Próximo hito desde [racha] — usado para el aviso de "casi llegas". */
fun proximoHitoRacha(racha: Int): Int = when {
    racha < 7 -> 7
    racha < 21 -> 21
    racha < 30 -> 30
    else -> ((racha / 30) + 1) * 30
}

/**
 * Emoji "cara" de la celebración de hito — una plantita que crece, el mismo símbolo que Lula ya
 * usa para "hábitos/crecimiento" (ver `TipoActividadEmoji.kt`) en vez de inventar un ícono
 * nuevo. Deja el camino listo para más adelante reemplazarlo por un personaje propio.
 */
fun emojiHitoRacha(racha: Int): String = when {
    racha < 21 -> "🌱"
    racha < 30 -> "🌿"
    else -> "🌳"
}

fun mensajeHitoRacha(racha: Int): String = when (racha) {
    7 -> MENSAJES_HITO_7.random()
    21 -> MENSAJES_HITO_21.random()
    30 -> MENSAJES_HITO_30.random()
    else -> mensajesHitoMultiplo(racha).random()
}

fun mensajeCierreDiario(): String = MENSAJES_CIERRE_DIARIO.random()

/**
 * Aviso de "casi llegas" cuando falta exactamente 1 día para el próximo hito — null si no
 * aplica. Se muestra en vez del mensaje diario normal, para dar una razón concreta de volver
 * mañana (a diferencia de una urgencia falsa, es información real).
 */
fun mensajeAnticipacionHito(racha: Int): String? {
    val proximo = proximoHitoRacha(racha)
    if (proximo - racha != 1) return null
    return MENSAJES_ANTICIPACION.random()(proximo)
}
