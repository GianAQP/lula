package com.aqpseller.lulaapp.features.proposito

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.PREGUNTAS_PROPOSITO
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.proposito.EliminarPropositoPersonalUseCase
import com.aqpseller.lulaapp.domain.usecase.proposito.ObtenerPropositoPersonalUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Mi propósito" (alcanzable desde Mi perfil) — lista las 13 preguntas guiadas para armar
 * Misión/Visión/Propósito de a poco, cada una editable por separado. Ver
 * `Plan/10-pendientes.md`, estrategia acordada 2026-07-30.
 */
@HiltViewModel
class PropositoPersonalViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerPropositoPersonalUseCase: ObtenerPropositoPersonalUseCase,
    private val eliminarPropositoPersonalUseCase: EliminarPropositoPersonalUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropositoPersonalUiState())
    val uiState: StateFlow<PropositoPersonalUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            obtenerPropositoPersonalUseCase(sesionActual.espacioPersonalId).collect { proposito ->
                val respuestas = proposito?.respuestas ?: emptyMap()
                _uiState.update {
                    it.copy(
                        cargando = false,
                        preguntas = PREGUNTAS_PROPOSITO.map { pregunta ->
                            PreguntaPropositoUi(
                                id = pregunta.id,
                                texto = pregunta.texto,
                                seccion = pregunta.seccion,
                                respuesta = respuestas[pregunta.id],
                            )
                        },
                    )
                }
            }
        }
    }

    fun eliminarTodo() {
        viewModelScope.launch {
            val sesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
            eliminarPropositoPersonalUseCase(sesionActual.espacioPersonalId, sesionActual.usuarioId)
        }
    }
}
