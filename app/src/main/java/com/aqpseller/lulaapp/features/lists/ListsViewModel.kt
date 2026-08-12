package com.aqpseller.lulaapp.features.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.lista.ActualizarOrdenListaUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.ObtenerListasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerListasUseCase: ObtenerListasUseCase,
    private val actualizarOrdenListaUseCase: ActualizarOrdenListaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            obtenerListasUseCase(sesionActual().espacioId).collect { listas ->
                _uiState.value = ListsUiState(cargando = false, listas = listas)
            }
        }
    }

    fun moverArriba(listaId: String) = mover(listaId, haciaArriba = true)
    fun moverAbajo(listaId: String) = mover(listaId, haciaArriba = false)

    /** Flechas ▲▼ para reordenar los títulos de lista — mismo patrón que Notas. Intercambia el
     * `orden` con el vecino inmediato en la lista ya ordenada que se ve en pantalla. */
    private fun mover(listaId: String, haciaArriba: Boolean) {
        val listas = _uiState.value.listas
        val index = listas.indexOfFirst { it.id == listaId }
        if (index < 0) return
        val indiceVecino = if (haciaArriba) index - 1 else index + 1
        if (indiceVecino !in listas.indices) return
        val actual = listas[index]
        val vecino = listas[indiceVecino]
        viewModelScope.launch {
            val usuarioId = sesionActual().usuarioId
            actualizarOrdenListaUseCase(actual.id, vecino.orden, usuarioId)
            actualizarOrdenListaUseCase(vecino.id, actual.orden, usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
