package com.aqpseller.lulaapp.features.health

import com.aqpseller.lulaapp.domain.model.EstadoActividad

data class CitaDetailUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val motivo: String? = null,
    val lugar: String? = null,
    val fechaHora: Long = 0L,
    val estado: EstadoActividad = EstadoActividad.SIN_CONFIRMAR,
    val nombresTareasVinculadas: List<String> = emptyList(),
    val eliminado: Boolean = false,
    val esCurso: Boolean = false,
    val sesiones: List<SesionCitaUi> = emptyList(),
    val cantidadSesionesTotal: Int? = null,
)

data class SesionCitaUi(
    val numeroSesion: Int,
    /** Epoch day. */
    val fecha: Long,
    val horario: String,
    val estado: EstadoActividad,
)
