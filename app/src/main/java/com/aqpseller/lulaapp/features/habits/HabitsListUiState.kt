package com.aqpseller.lulaapp.features.habits

import com.aqpseller.lulaapp.domain.model.MomentoDelDia

/** Un día del tracker semanal de un hábito — letra (L/M/M/J/V/S/D), si se cumplió, y si es hoy
 * (para resaltarlo aunque esté vacío, ver `Plan/08-decisiones-tecnicas.md`). */
data class DiaTrackerUi(
    val letra: String,
    val confirmado: Boolean,
    val esHoy: Boolean,
)

data class HabitoListItemUi(
    val id: String,
    val nombre: String,
    val emoji: String,
    val momentoDelDia: MomentoDelDia,
    /** Racha propia de este hábito — a diferencia de la racha global de `LulaTopBar` (que
     * cuenta días con "Cerrar mi día" hecho), esta SIEMPRE cuadra con los círculos que se ven
     * debajo, porque se calcula del mismo historial. */
    val racha: Int,
    /** Últimos 7 días, de más antiguo a hoy. */
    val diasSemana: List<DiaTrackerUi>,
)

data class HabitsListUiState(
    val cargando: Boolean = true,
    val habitos: List<HabitoListItemUi> = emptyList(),
) {
    val porMomento: Map<MomentoDelDia, List<HabitoListItemUi>>
        get() = habitos.groupBy { it.momentoDelDia }

    private val fraccionSemana: Float
        get() {
            val total = habitos.sumOf { it.diasSemana.size }
            if (total == 0) return 0f
            val confirmados = habitos.sumOf { habito -> habito.diasSemana.count { it.confirmado } }
            return confirmados.toFloat() / total
        }

    /** Nunca negativo ni de "castigo" (filosofía "todo intento vale" — ver `Plan/CLAUDE.md`),
     * ni siquiera cuando la fracción es 0. */
    val mensajeMotivacional: String
        get() = when {
            fraccionSemana >= 0.7f -> "Vas muy bien esta semana 💪"
            fraccionSemana > 0f -> "Cada intento cuenta, sigue así 🌱"
            else -> "Hoy es un buen día para empezar 🙂"
        }
}
