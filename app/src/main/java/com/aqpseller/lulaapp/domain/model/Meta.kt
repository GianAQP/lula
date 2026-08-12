package com.aqpseller.lulaapp.domain.model

data class Meta(
    val id: String,
    val espacioId: String,
    val nombre: String,
    val areaDeVidaId: String?,
    val fechaLimite: Long?,
    val comoSeMide: ComoSeMideMeta,
    /** POR_HABITO: días objetivo (ventana móvil). Resto: cantidad objetivo. */
    val valorObjetivo: Double,
    /** Ignorado para POR_HABITO — se calcula en vivo desde el historial del hábito vinculado. */
    val valorActual: Double,
    val actividadesVinculadasIds: List<String>,
    /** Último hito (0/25/50/75/100) ya celebrado en Hoy — ver `ObtenerMetasConProgresoUseCase`. */
    val ultimoHitoCelebrado: Int = 0,
    /** A cuál de las 6 preguntas de ayuda responde esta meta (opcional) — permite agrupar las
     * metas completadas por categoría en vez de solo una lista plana. Ver `CategoriaMeta`,
     * `Plan/08-decisiones-tecnicas.md`. */
    val categoria: CategoriaMeta? = null,
    /** Si además de mostrarse en Metas, debe sonar un aviso el día que llega `fechaLimite` —
     * ignorado si `fechaLimite` es null. */
    val avisarAlVencer: Boolean = false,
)

/**
 * Las 6 preguntas de ayuda para pensar una meta (`CrearMetaScreen`) — el nombre de cada entrada
 * es también la categoría con la que se etiqueta la meta, para poder agrupar las completadas
 * por categoría más adelante. Ver `Plan/08-decisiones-tecnicas.md`.
 */
enum class CategoriaMeta {
    HACER, SER, VER, TENER, IR, COMPARTIR
}
