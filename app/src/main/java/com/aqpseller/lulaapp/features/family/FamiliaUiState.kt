package com.aqpseller.lulaapp.features.family

data class EspacioUi(
    val id: String,
    val nombre: String,
    val esFamilia: Boolean,
    val esActivo: Boolean,
)

data class MiembroUi(val nombre: String, val rol: String)

data class FamiliaUiState(
    val cargando: Boolean = true,
    val espacios: List<EspacioUi> = emptyList(),
    val espacioFamiliaId: String? = null,
    val nombreEspacioFamilia: String = "",
    val miembros: List<MiembroUi> = emptyList(),
    val mostrarFormularioCrear: Boolean = false,
    val mostrarFormularioRenombrar: Boolean = false,
    /** Se puso true tras crear o cambiar de espacio — la pantalla vuelve sola a Hoy. */
    val espacioCambiado: Boolean = false,
)
