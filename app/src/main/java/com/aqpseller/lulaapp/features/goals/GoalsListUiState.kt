package com.aqpseller.lulaapp.features.goals

import com.aqpseller.lulaapp.domain.model.CategoriaMeta

data class MetaListItemUi(
    val id: String,
    val nombre: String,
    val progreso: Double,
    val objetivo: Double,
    val nombreHabitoVinculado: String?,
    val fechaLimite: Long?,
    val categoria: CategoriaMeta?,
) {
    val fraccionProgreso: Float
        get() = if (objetivo > 0) (progreso / objetivo).toFloat().coerceIn(0f, 1f) else 0f
}

/** Un grupo de metas de la misma categoría (una de las 6 preguntas de ayuda), en progreso y
 * completadas juntas — a pedido del usuario, para que se puedan repasar todas las metas de un
 * vistazo agrupadas como se pensaron desde el principio, no separadas en dos listas. `categoria
 * = null` agrupa las que no se etiquetaron, siempre al final. Ver `Plan/08-decisiones-tecnicas.md`. */
data class GrupoMetasUi(
    val categoria: CategoriaMeta?,
    val metas: List<MetaListItemUi>,
)

data class GoalsListUiState(
    val cargando: Boolean = true,
    val totalMetas: Int = 0,
    val totalCompletadas: Int = 0,
    val grupos: List<GrupoMetasUi> = emptyList(),
)
