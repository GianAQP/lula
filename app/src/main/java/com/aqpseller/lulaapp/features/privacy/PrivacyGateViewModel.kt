package com.aqpseller.lulaapp.features.privacy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.security.SesionPrivadaState
import com.aqpseller.lulaapp.core.security.biometriaDisponible
import com.aqpseller.lulaapp.domain.repository.PrivacidadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyGateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privacidadRepository: PrivacidadRepository,
    private val sesionPrivadaState: SesionPrivadaState,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyGateUiState())
    val uiState: StateFlow<PrivacyGateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val configurada = privacidadRepository.estaConfigurada()
            _uiState.update {
                it.copy(cargando = false, configurada = configurada, biometriaDisponible = biometriaDisponible(context))
            }
        }
        viewModelScope.launch {
            sesionPrivadaState.desbloqueada.collect { desbloqueada ->
                _uiState.update { it.copy(desbloqueada = desbloqueada) }
            }
        }
    }

    fun configurarPin(pin: String) {
        if (pin.length < 4) return
        viewModelScope.launch {
            privacidadRepository.configurarPin(pin)
            sesionPrivadaState.desbloquear()
        }
    }

    fun intentarPin(pin: String) {
        viewModelScope.launch {
            if (privacidadRepository.verificarPin(pin)) {
                sesionPrivadaState.desbloquear()
            } else {
                _uiState.update { it.copy(pinIncorrecto = true) }
            }
        }
    }

    fun desbloquearConBiometria() {
        sesionPrivadaState.desbloquear()
    }
}
