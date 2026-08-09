package com.aqpseller.lulaapp.features.goals

data class MetaListItemUi(
    val id: String,
    val nombre: String,
    val progreso: Double,
    val objetivo: Double,
    val nombreHabitoVinculado: String?,
    val fechaLimite: Long?,
) {
    val fraccionProgreso: Float
        get() = if (objetivo > 0) (progreso / objetivo).toFloat().coerceIn(0f, 1f) else 0f
}

data class GoalsListUiState(
    val cargando: Boolean = true,
    val metas: List<MetaListItemUi> = emptyList(),
)
