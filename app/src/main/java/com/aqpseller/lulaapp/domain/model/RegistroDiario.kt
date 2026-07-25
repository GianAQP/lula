package com.aqpseller.lulaapp.domain.model

data class RegistroDiario(
    val id: String,
    val espacioId: String,
    val fecha: Long,
    val actividadesCompletadas: Int,
    val actividadesTotales: Int,
    val puntuacion: Int,
    val estadoAnimo: String? = null,
    val queLogre: String? = null,
    val queCosto: String? = null,
    val queAjusto: String? = null,
)

data class RegistroSemanal(
    val id: String,
    val espacioId: String,
    val semana: String,
    val cumplimientoGeneralPorcentaje: Int,
    val rachaMaxima: Int,
    val queLogre: String? = null,
    val queNoFunciono: String? = null,
    val queAjusto: String? = null,
)
