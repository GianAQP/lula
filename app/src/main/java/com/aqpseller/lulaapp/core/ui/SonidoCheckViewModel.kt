package com.aqpseller.lulaapp.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Único punto de lectura de la preferencia "sonido al marcar un check" (Ajustes) — cualquier
 * pantalla con un `Checkbox` de Hábito/Tarea/Rutina/Lista lo usa con
 * `hiltViewModel<SonidoCheckViewModel>()` en vez de duplicar la inyección de
 * `AjustesRepository`. Regla general: todo check nuevo en la app debe respetar este ajuste,
 * ver `Plan/08-decisiones-tecnicas.md`.
 */
@HiltViewModel
class SonidoCheckViewModel @Inject constructor(
    ajustesRepository: AjustesRepository,
) : ViewModel() {
    val habilitado: StateFlow<Boolean> = ajustesRepository.observarSonidoCheckHabilitado()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
}
