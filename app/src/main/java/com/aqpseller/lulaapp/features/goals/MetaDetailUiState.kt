package com.aqpseller.lulaapp.features.goals

import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta

data class MetaDetailUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val comoSeMide: ComoSeMideMeta = ComoSeMideMeta.MANUAL,
    val progreso: Double = 0.0,
    val objetivo: Double = 0.0,
    val nombreHabitoVinculado: String? = null,
    val eliminada: Boolean = false,
) {
    val fraccionProgreso: Float
        get() = if (objetivo > 0) (progreso / objetivo).toFloat().coerceIn(0f, 1f) else 0f

    /** POR_HABITO y POR_MONTO calculan el progreso solos — el botón "+ Agregar progreso" solo tiene sentido para el resto. */
    val esManual: Boolean get() = comoSeMide == ComoSeMideMeta.POR_NUMERO || comoSeMide == ComoSeMideMeta.MANUAL

    val esPorMonto: Boolean get() = comoSeMide == ComoSeMideMeta.POR_MONTO
}
