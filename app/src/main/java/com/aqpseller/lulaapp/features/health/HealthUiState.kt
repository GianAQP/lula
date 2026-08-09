package com.aqpseller.lulaapp.features.health

import com.aqpseller.lulaapp.domain.model.EstadoActividad

data class TomaUi(
    val actividadId: String,
    val horario: String,
    val instruccion: String,
    val estado: EstadoActividad,
)

data class MedicamentoUi(
    val actividadId: String,
    val nombre: String,
    val dosis: String,
    val tomasHoy: List<TomaUi>,
)

data class CitaUi(
    val actividadId: String,
    val nombre: String,
    val fechaHora: Long,
    val lugar: String?,
    /** Curso (varias sesiones, ej. radioterapia) — `fechaHora` es la de la PRÓXIMA sesión sin
     * marcar, no la del curso completo, así que una que ya empezó sigue apareciendo acá. */
    val esCurso: Boolean = false,
    val progresoTexto: String? = null,
)

data class HealthUiState(
    val cargando: Boolean = true,
    val medicamentos: List<MedicamentoUi> = emptyList(),
    val proximasCitas: List<CitaUi> = emptyList(),
) {
    val hayAlgo: Boolean get() = medicamentos.isNotEmpty() || proximasCitas.isNotEmpty()
}
