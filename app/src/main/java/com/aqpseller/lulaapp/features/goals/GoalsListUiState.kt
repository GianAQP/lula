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

/** Un grupo de metas ya completadas, todas de la misma categoría — ver una a una cuántas se
 * lograron por categoría refuerza la sensación de avance más que una sola lista plana (pedido
 * explícito del usuario). `categoria = null` agrupa las que no se etiquetaron. Ver
 * `Plan/08-decisiones-tecnicas.md`. */
data class GrupoMetasCompletadasUi(
    val categoria: CategoriaMeta?,
    val metas: List<MetaListItemUi>,
)

data class GoalsListUiState(
    val cargando: Boolean = true,
    val metasEnProgreso: List<MetaListItemUi> = emptyList(),
    val gruposCompletadas: List<GrupoMetasCompletadasUi> = emptyList(),
) {
    val totalCompletadas: Int get() = gruposCompletadas.sumOf { it.metas.size }
}
