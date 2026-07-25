package com.aqpseller.lulaapp.domain.model

data class Meta(
    val id: String,
    val espacioId: String,
    val nombre: String,
    val areaDeVidaId: String?,
    val fechaLimite: Long?,
    val comoSeMide: ComoSeMideMeta,
    val actividadesVinculadasIds: List<String>,
)
