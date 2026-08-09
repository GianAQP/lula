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
)
