package com.aqpseller.lulaapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.usecase.usuario.AsegurarDatosSemillaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gate de arranque: siembra el usuario/espacio/áreas locales en el primer inicio
 * (idempotente) antes de componer la navegación real, para que ninguna pantalla consulte
 * un `espacioId` que todavía no existe.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val asegurarDatosSemillaUseCase: AsegurarDatosSemillaUseCase,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            asegurarDatosSemillaUseCase()
            _isReady.value = true
        }
    }
}
