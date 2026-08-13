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
    /** A cuál de las 6 preguntas de ayuda responde esta meta — el primer paso al crearla
     * (`CrearMetaScreen`), permite agrupar las metas por categoría tanto en progreso como
     * completadas. Ver `CategoriaMeta`, `Plan/08-decisiones-tecnicas.md`. */
    val categoria: CategoriaMeta? = null,
    /** Qué tan insistente es el aviso el día que llega `fechaLimite` — igual que Hábito/Tarea/
     * Medicamento/Cita, siempre tiene un nivel (nunca "apagado": Silencioso ya es la opción de
     * bajo perfil). Ignorado si `fechaLimite` es null. */
    val nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
)

/**
 * Las 6 preguntas de ayuda para pensar una meta (`CrearMetaScreen`) — el nombre de cada entrada
 * es también la categoría con la que se etiqueta la meta, para poder agrupar las completadas
 * por categoría más adelante. Ver `Plan/08-decisiones-tecnicas.md`.
 */
enum class CategoriaMeta {
    HACER, SER, VER, TENER, IR, COMPARTIR
}
