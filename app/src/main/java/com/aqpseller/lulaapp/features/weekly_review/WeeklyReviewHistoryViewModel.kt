package com.aqpseller.lulaapp.features.weekly_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.usecase.registrosemanal.ObtenerHistorialSemanalUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeeklyReviewHistoryViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerHistorialSemanalUseCase: ObtenerHistorialSemanalUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReviewHistoryUiState())
    val uiState: StateFlow<WeeklyReviewHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            obtenerHistorialSemanalUseCase(sesion.espacioId)
                .map { lista -> lista.map { it.aUi() } }
                .collect { semanas -> _uiState.value = WeeklyReviewHistoryUiState(cargando = false, semanas = semanas) }
        }
    }

    private fun RegistroSemanal.aUi() = RevisionSemanalHistorialItemUi(
        semana = semana,
        etiqueta = "Semana del " + DateTimeUtils.formatearFechaLarga(LocalDate.parse(semana)),
        cumplimientoPorcentaje = cumplimientoGeneralPorcentaje,
        rachaMaxima = rachaMaxima,
        queLogre = queLogre,
        queNoFunciono = queNoFunciono,
        queAjusto = queAjusto,
    )
}
