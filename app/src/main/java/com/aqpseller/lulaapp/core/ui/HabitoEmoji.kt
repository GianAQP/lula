package com.aqpseller.lulaapp.core.ui

/**
 * Ícono automático por palabra clave en el nombre del hábito — puramente cosmético (ayuda a
 * distinguir tarjetas de un vistazo, a pedido del usuario), nunca se guarda ni afecta lógica de
 * negocio. Si ninguna palabra coincide, usa un ícono neutro. Ver `Plan/08-decisiones-tecnicas.md`.
 */
private val PALABRAS_CLAVE_EMOJI_HABITO: List<Pair<List<String>, String>> = listOf(
    listOf("cama", "tender") to "🛏️",
    listOf("desayun") to "🍳",
    listOf("almuerz", "comida", "comer") to "🍽️",
    listOf("cen") to "🌙",
    listOf("agua", "hidrat") to "💧",
    listOf("ejercicio", "gym", "gimnasio", "entren") to "🏋️",
    listOf("correr", "caminar", "trote", "trotar") to "🏃",
    listOf("leer", "lectura", "libro") to "📖",
    listOf("medit") to "🧘",
    listOf("bañ", "ducha") to "🚿",
    listOf("diente", "cepill") to "🦷",
    listOf("dormir", "descans") to "😴",
    listOf("estudi") to "📚",
    listOf("trabaj") to "💼",
    listOf("orar", "rezar", "oraci") to "🙏",
    listOf("estir", "yoga") to "🤸",
    listOf("vitamin", "pastilla", "suplement") to "💊",
    listOf("escrib", "diario", "journal") to "✍️",
    listOf("limpi", "orden") to "🧹",
    listOf("plant", "jardin", "regar") to "🌱",
    listOf("music", "tocar", "instrumento") to "🎵",
)

fun emojiParaHabito(nombre: String): String {
    val texto = nombre.lowercase()
    return PALABRAS_CLAVE_EMOJI_HABITO.firstOrNull { (palabras, _) -> palabras.any { texto.contains(it) } }?.second ?: "✅"
}
