package com.aqpseller.lulaapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.security.SesionPrivadaState
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.usecase.notificaciones.ReprogramarTodosLosRecordatoriosUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.AsegurarDatosSemillaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val reprogramarTodosLosRecordatoriosUseCase: ReprogramarTodosLosRecordatoriosUseCase,
    sesionPrivadaState: SesionPrivadaState,
    ajustesRepository: AjustesRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** Para que `LulaNavHost` saque al usuario de una pantalla de Zona Privada si se re-bloquea sola. */
    val zonaPrivadaDesbloqueada: StateFlow<Boolean> = sesionPrivadaState.desbloqueada

    /** Qué va en cada posición configurable de la barra inferior — ver `OpcionBottomBar`. */
    val bottomBarPosicion2: StateFlow<String> = ajustesRepository.observarBottomBarPosicion2()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "asistente")
    val bottomBarPosicion3: StateFlow<String> = ajustesRepository.observarBottomBarPosicion3()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "habitos")
    val bottomBarPosicion4: StateFlow<String> = ajustesRepository.observarBottomBarPosicion4()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "finanzas")

    init {
        viewModelScope.launch {
            asegurarDatosSemillaUseCase()
            _isReady.value = true
        }
        // Aparte, sin bloquear `isReady` — algunos fabricantes (Motorola incluido) "fuerzan
        // detener" la app en segundo plano para ahorrar batería, lo que cancela todas las
        // alarmas pendientes sin pasar por un reinicio del teléfono (que es el único momento en
        // que `BootReceiver` las repara). Reprogramar todo cada vez que se abre la app es la
        // forma de que "abrir Lula de nuevo" alcance para reparar los recordatorios, sin
        // esperar a un reinicio real. Ver `Plan/08-decisiones-tecnicas.md`.
        viewModelScope.launch {
            runCatching { reprogramarTodosLosRecordatoriosUseCase() }
        }
    }
}
