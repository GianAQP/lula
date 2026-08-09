package com.aqpseller.lulaapp.features.weekly_review

data class HabitoDestacadoUi(val nombre: String, val porcentaje: Int)

data class WeeklyReviewUiState(
    val cargando: Boolean = true,
    /** Falso si hoy todavía no llega al día configurado en Ajustes — se muestra un estado de espera. */
    val activada: Boolean = true,
    val nombreDiaActivacion: String = "",
    val cumplimientoPorcentaje: Int = 0,
    val rachaMaxima: Int = 0,
    val mejorHabito: HabitoDestacadoUi? = null,
    val peorHabito: HabitoDestacadoUi? = null,
    val guardada: Boolean = false,
    /** Transitorio — se pone true justo al guardar, para que la pantalla vuelva sola atrás y
     * confirme que se guardó (antes no pasaba nada visible y el usuario tocaba "Guardar" varias
     * veces sin saber si había funcionado). */
    val guardadoExitoso: Boolean = false,
    val queLogreGuardado: String = "",
    val queNoFuncionoGuardado: String = "",
    val queAjustoGuardado: String = "",
)
