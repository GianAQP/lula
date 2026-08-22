package com.aqpseller.lulaapp.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.retofamiliar.MarcarRetoFamiliarCumplidoUseCase
import com.aqpseller.lulaapp.domain.usecase.retofamiliar.ObtenerRetosFamiliaresUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RetosFamiliaresViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerRetosFamiliaresUseCase: ObtenerRetosFamiliaresUseCase,
    private val marcarRetoFamiliarCumplidoUseCase: MarcarRetoFamiliarCumplidoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RetosFamiliaresUiState())
    val uiState: StateFlow<RetosFamiliaresUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            obtenerRetosFamiliaresUseCase(sesionActual.espacioId, sesionActual.usuarioId).collect { progresos ->
                _uiState.update {
                    it.copy(
                        cargando = false,
                        retos = progresos.map { progreso ->
                            RetoFamiliarUi(
                                id = progreso.reto.id,
                                nombre = progreso.reto.nombre,
                                objetivo = progreso.reto.objetivo,
                                recompensa = progreso.reto.recompensa,
                                cumplidosHoy = progreso.cumplidosHoy,
                                totalParticipantes = progreso.totalParticipantes,
                                yoCumpliHoy = progreso.yoCumpliHoy,
                            )
                        },
                    )
                }
            }
        }
    }

    fun marcarCumplidoHoy(retoId: String, cumplido: Boolean) {
        val sesionActual = sesion ?: return
        viewModelScope.launch {
            marcarRetoFamiliarCumplidoUseCase(sesionActual.espacioId, retoId, sesionActual.usuarioId, cumplido)
        }
    }
}
