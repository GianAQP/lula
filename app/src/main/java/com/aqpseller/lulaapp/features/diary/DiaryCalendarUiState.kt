package com.aqpseller.lulaapp.features.diary

import kotlinx.datetime.LocalDate

data class DiaDiarioUi(
    val fecha: LocalDate,
    /** Id de la entrada de ese día si existe (la más reciente, si hubiera más de una). Null = día vacío. */
    val entradaId: String?,
    val esHoy: Boolean,
    val esDelMesVisible: Boolean,
)

data class DiaryCalendarUiState(
    val cargando: Boolean = true,
    val mesVisible: LocalDate,
    val dias: List<DiaDiarioUi> = emptyList(),
)
