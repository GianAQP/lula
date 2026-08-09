package com.aqpseller.lulaapp.features.weekly_review

data class RevisionSemanalHistorialItemUi(
    val semana: String,
    val etiqueta: String,
    val cumplimientoPorcentaje: Int,
    val rachaMaxima: Int,
    val queLogre: String?,
    val queNoFunciono: String?,
    val queAjusto: String?,
)

data class WeeklyReviewHistoryUiState(
    val cargando: Boolean = true,
    val semanas: List<RevisionSemanalHistorialItemUi> = emptyList(),
)
