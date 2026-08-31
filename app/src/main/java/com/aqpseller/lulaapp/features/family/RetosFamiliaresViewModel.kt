package com.aqpseller.lulaapp.features.family

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** El espacio de los retos es el que trae la navegación (`espacioId`, ver `LulaDestinations`),
 * no necesariamente el espacio ACTIVO de la app — así se puede administrar los retos de
 * cualquiera de tus Familias sin tener que cambiarte de espacio de trabajo primero. Ver
 * `Plan/08-decisiones-tecnicas.md`. */
@HiltViewModel
class RetosFamiliaresViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerRetosFamiliaresUseCase: ObtenerRetosFamiliaresUseCase,
    private val marcarRetoFamiliarCumplidoUseCase: MarcarRetoFamiliarCumplidoUseCase,
) : ViewModel() {

    private val espacioId: String = checkNotNull(savedStateHandle["espacioId"])

    private val _uiState = MutableStateFlow(RetosFamiliaresUiState())
    val uiState: StateFlow<RetosFamiliaresUiState> = _uiState.asStateFlow()

    private var miUsuarioId: String? = null

    init {
        viewModelScope.launch {
            miUsuarioId = obtenerSesionActualUseCase().usuarioId
            obtenerRetosFamiliaresUseCase(espacioId, miUsuarioId!!).collect { progresos ->
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
        val usuarioId = miUsuarioId ?: return
        viewModelScope.launch {
            marcarRetoFamiliarCumplidoUseCase(espacioId, retoId, usuarioId, cumplido)
        }
    }
}
