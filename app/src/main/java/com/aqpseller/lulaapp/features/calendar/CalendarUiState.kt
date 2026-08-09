package com.aqpseller.lulaapp.features.calendar

import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import kotlinx.datetime.LocalDate

enum class ModoCalendario { DIA, SEMANA, MES }

data class DiaCalendarioUi(
    val fecha: LocalDate,
    val items: List<ItemAgenda>,
    val esHoy: Boolean,
    val esDelMesVisible: Boolean,
)

data class CalendarUiState(
    val cargando: Boolean = true,
    val modo: ModoCalendario = ModoCalendario.DIA,
    val fechaSeleccionada: LocalDate,
    val diasVisibles: List<DiaCalendarioUi> = emptyList(),
    /** Cierre del día ("Cerrar mi día") de `fechaSeleccionada`, solo relevante en modo DIA —
     * null si ese día no se cerró nunca (no se muestra nada, ver `08-decisiones-tecnicas.md`). */
    val registroDelDia: RegistroDiario? = null,
)
