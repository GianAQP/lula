package com.aqpseller.lulaapp.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.ui.emojiParaHabito
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHabitosUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHistorialHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DIAS_TRACKER_SEMANAL = 7

@HiltViewModel
class HabitsListViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerHabitosUseCase: ObtenerHabitosUseCase,
    private val obtenerHistorialHabitoUseCase: ObtenerHistorialHabitoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsListUiState())
    val uiState: StateFlow<HabitsListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val hoyEpochDay = DateTimeUtils.hoy().toEpochDays().toLong()
            obtenerHabitosUseCase(sesion.espacioId)
                .map { habitos ->
                    habitos.map { habito ->
                        val historial = obtenerHistorialHabitoUseCase.ultimosDias(habito.id, DIAS_TRACKER_SEMANAL)
                        val racha = obtenerHistorialHabitoUseCase.calcularRacha(habito.id)
                        HabitoListItemUi(
                            id = habito.id,
                            nombre = habito.nombre,
                            emoji = emojiParaHabito(habito.nombre),
                            momentoDelDia = habito.momentoDelDia ?: MomentoDelDia.MANANA,
                            racha = racha,
                            diasSemana = historial.map { dia ->
                                DiaTrackerUi(
                                    letra = DateTimeUtils.letraDiaSemana(DateTimeUtils.epochDiasAEpochMillis(dia.fecha)),
                                    confirmado = dia.estado == EstadoActividad.CONFIRMADO,
                                    esHoy = dia.fecha == hoyEpochDay,
                                )
                            },
                        )
                    }
                }
                .collect { habitos -> _uiState.value = HabitsListUiState(cargando = false, habitos = habitos) }
        }
    }
}
