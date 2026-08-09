package com.aqpseller.lulaapp.features.proposito

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.PREGUNTAS_PROPOSITO
import com.aqpseller.lulaapp.domain.model.PreguntaProposito
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.proposito.EliminarRespuestaPropositoUseCase
import com.aqpseller.lulaapp.domain.usecase.proposito.GuardarRespuestaPropositoUseCase
import com.aqpseller.lulaapp.domain.usecase.proposito.ObtenerPropositoPersonalUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditarRespuestaPropositoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerPropositoPersonalUseCase: ObtenerPropositoPersonalUseCase,
    private val guardarRespuestaPropositoUseCase: GuardarRespuestaPropositoUseCase,
    private val eliminarRespuestaPropositoUseCase: EliminarRespuestaPropositoUseCase,
) : ViewModel() {

    private val preguntaId: String = checkNotNull(savedStateHandle["preguntaId"])
    val pregunta: PreguntaProposito = PREGUNTAS_PROPOSITO.first { it.id == preguntaId }

    private val _respuestaInicial = MutableStateFlow<String?>(null)
    val respuestaInicial: StateFlow<String?> = _respuestaInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _eliminada = MutableStateFlow(false)
    val eliminada: StateFlow<Boolean> = _eliminada.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            val proposito = obtenerPropositoPersonalUseCase(sesionActual.espacioPersonalId).first()
            _respuestaInicial.value = proposito?.respuestas?.get(preguntaId).orEmpty()
        }
    }

    fun guardar(respuesta: String) {
        viewModelScope.launch {
            val sesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
            guardarRespuestaPropositoUseCase(sesionActual.espacioPersonalId, sesionActual.usuarioId, preguntaId, respuesta)
            _guardado.value = true
        }
    }

    fun eliminar() {
        viewModelScope.launch {
            val sesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
            eliminarRespuestaPropositoUseCase(sesionActual.espacioPersonalId, sesionActual.usuarioId, preguntaId)
            _eliminada.value = true
        }
    }
}
