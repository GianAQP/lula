package com.aqpseller.lulaapp.features.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.calendario.ObtenerAgendaDelRangoUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerRegistroDiarioDeFechaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerAgendaDelRangoUseCase: ObtenerAgendaDelRangoUseCase,
    private val obtenerRegistroDiarioDeFechaUseCase: ObtenerRegistroDiarioDeFechaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(fechaSeleccionada = DateTimeUtils.hoy()))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            cargar()
        }
    }

    fun cambiarModo(modo: ModoCalendario) {
        _uiState.update { it.copy(modo = modo) }
        recargar()
    }

    fun anterior() = mover(-1)
    fun siguiente() = mover(1)

    fun irAHoy() {
        _uiState.update { it.copy(fechaSeleccionada = DateTimeUtils.hoy()) }
        recargar()
    }

    fun seleccionarFecha(fecha: LocalDate) {
        _uiState.update { it.copy(fechaSeleccionada = fecha, modo = ModoCalendario.DIA) }
        recargar()
    }

    private fun mover(direccion: Int) {
        val actual = _uiState.value
        val nuevaFecha = when (actual.modo) {
            ModoCalendario.DIA -> actual.fechaSeleccionada.plus(DatePeriod(days = direccion))
            ModoCalendario.SEMANA -> actual.fechaSeleccionada.plus(DatePeriod(days = 7 * direccion))
            ModoCalendario.MES -> agregarMeses(actual.fechaSeleccionada, direccion)
        }
        _uiState.update { it.copy(fechaSeleccionada = nuevaFecha) }
        recargar()
    }

    private fun recargar() {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val sesionActual = sesionActual()
        val estado = _uiState.value
        val (desde, hasta) = rangoVisible(estado.fechaSeleccionada, estado.modo)
        val agenda = obtenerAgendaDelRangoUseCase(sesionActual.espacioId, desde, hasta)
        val hoy = DateTimeUtils.hoy()
        val mesVisible = estado.fechaSeleccionada.monthNumber
        val dias = generarSecuenciaFechas(desde, hasta).map { fecha ->
            DiaCalendarioUi(
                fecha = fecha,
                items = agenda[fecha].orEmpty(),
                esHoy = fecha == hoy,
                esDelMesVisible = fecha.monthNumber == mesVisible,
            )
        }
        val registroDelDia = if (estado.modo == ModoCalendario.DIA) {
            obtenerRegistroDiarioDeFechaUseCase(sesionActual.espacioId, estado.fechaSeleccionada.toEpochDays().toLong())
        } else {
            null
        }
        _uiState.update { it.copy(cargando = false, diasVisibles = dias, registroDelDia = registroDelDia) }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}

private fun agregarMeses(fecha: LocalDate, cantidad: Int): LocalDate {
    // Se fija al día 1 antes de sumar meses para no romper con "31 de marzo + 1 mes" (abril no
    // tiene 31) — el Calendario solo necesita saber en qué mes está, no un día exacto.
    return LocalDate(fecha.year, fecha.monthNumber, 1).plus(DatePeriod(months = cantidad))
}

private fun generarSecuenciaFechas(desde: LocalDate, hasta: LocalDate): List<LocalDate> =
    generateSequence(desde) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= hasta }.toList()

/** Rango de fechas a cargar según el modo — Mes trae la grilla completa (semanas parciales incluidas). */
private fun rangoVisible(fecha: LocalDate, modo: ModoCalendario): Pair<LocalDate, LocalDate> = when (modo) {
    ModoCalendario.DIA -> fecha to fecha
    ModoCalendario.SEMANA -> {
        val inicio = DateTimeUtils.epochDaysToLocalDate(DateTimeUtils.inicioDeSemanaEpochDias(fecha))
        val fin = DateTimeUtils.epochDaysToLocalDate(DateTimeUtils.finDeSemanaEpochDias(fecha))
        inicio to fin
    }
    ModoCalendario.MES -> {
        val primerDiaDelMes = LocalDate(fecha.year, fecha.monthNumber, 1)
        val ultimoDiaDelMes = primerDiaDelMes.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val inicioGrid = DateTimeUtils.epochDaysToLocalDate(DateTimeUtils.inicioDeSemanaEpochDias(primerDiaDelMes))
        val finGrid = DateTimeUtils.epochDaysToLocalDate(DateTimeUtils.finDeSemanaEpochDias(ultimoDiaDelMes))
        inicioGrid to finGrid
    }
}
