package com.aqpseller.lulaapp.features.home

import com.aqpseller.lulaapp.domain.model.EstadoActividad

data class ActividadUi(
    val id: String,
    val nombre: String,
    val estado: EstadoActividad,
)

data class HomeUiState(
    val cargando: Boolean = true,
    val racha: Int = 0,
    val diaYaCerrado: Boolean = false,
    val actividadesManana: List<ActividadUi> = emptyList(),
    val actividadesTarde: List<ActividadUi> = emptyList(),
    val actividadesNoche: List<ActividadUi> = emptyList(),
    val tareasDeHoy: List<ActividadUi> = emptyList(),
    val gastosHoyTotal: Double = 0.0,
) {
    val totalActividades: Int
        get() = actividadesManana.size + actividadesTarde.size + actividadesNoche.size + tareasDeHoy.size

    val completadas: Int
        get() = (actividadesManana + actividadesTarde + actividadesNoche + tareasDeHoy)
            .count { it.estado == EstadoActividad.CONFIRMADO }

    val hayAlgoParaHoy: Boolean
        get() = totalActividades > 0
}
