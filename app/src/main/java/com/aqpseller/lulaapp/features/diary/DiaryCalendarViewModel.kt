package com.aqpseller.lulaapp.features.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.diario.ObtenerEntradasDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class DiaryCalendarViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerEntradasDiarioUseCase: ObtenerEntradasDiarioUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryCalendarUiState(mesVisible = DateTimeUtils.hoy()))
    val uiState: StateFlow<DiaryCalendarUiState> = _uiState.asStateFlow()

    private var entradasPorFecha: Map<LocalDate, List<EntradaDiario>> = emptyMap()

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            obtenerEntradasDiarioUseCase(sesionActual.espacioPersonalId).collect { entradas ->
                entradasPorFecha = entradas.groupBy { DateTimeUtils.epochMillisToLocalDate(it.fecha) }
                recalcularGrilla()
            }
        }
    }

    fun mesAnterior() = cambiarMes(-1)
    fun mesSiguiente() = cambiarMes(1)

    fun irAHoy() {
        _uiState.update { it.copy(mesVisible = DateTimeUtils.hoy()) }
        recalcularGrilla()
    }

    private fun cambiarMes(delta: Int) {
        _uiState.update { it.copy(mesVisible = DateTimeUtils.agregarMeses(it.mesVisible, delta)) }
        recalcularGrilla()
    }

    private fun recalcularGrilla() {
        val mesVisible = _uiState.value.mesVisible
        val (desde, hasta) = DateTimeUtils.rangoGrillaMes(mesVisible)
        val hoy = DateTimeUtils.hoy()
        val dias = DateTimeUtils.secuenciaFechas(desde, hasta).map { fecha ->
            DiaDiarioUi(
                fecha = fecha,
                entradaId = entradasPorFecha[fecha]?.maxByOrNull { it.fecha }?.id,
                esHoy = fecha == hoy,
                esDelMesVisible = fecha.monthNumber == mesVisible.monthNumber,
            )
        }
        _uiState.update { it.copy(cargando = false, dias = dias) }
    }
}
