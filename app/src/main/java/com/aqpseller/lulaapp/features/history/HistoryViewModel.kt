package com.aqpseller.lulaapp.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerHistorialDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerHistorialDiarioUseCase: ObtenerHistorialDiarioUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()

            val constancia = obtenerProgresoDeHoyUseCase.calcularConstancia(sesion.espacioId)
            _uiState.update { it.copy(constancia = constancia) }

            obtenerHistorialDiarioUseCase(sesion.espacioId)
                .map { registros ->
                    registros.map { registro ->
                        RegistroDiarioUi(
                            id = registro.id,
                            fechaTexto = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochDaysToLocalDate(registro.fecha)),
                            actividadesCompletadas = registro.actividadesCompletadas,
                            actividadesTotales = registro.actividadesTotales,
                            puntuacion = registro.puntuacion,
                            queLogre = registro.queLogre,
                            queCosto = registro.queCosto,
                            queAjusto = registro.queAjusto,
                        )
                    }
                }
                .collect { registros -> _uiState.update { it.copy(cargando = false, registros = registros) } }
        }
    }
}
