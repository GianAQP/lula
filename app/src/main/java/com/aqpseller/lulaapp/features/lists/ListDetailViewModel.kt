package com.aqpseller.lulaapp.features.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.lista.AgregarItemListaUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.EliminarItemListaUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.EliminarListaUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.MarcarItemListaUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.ObtenerListaConItemsUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.ReiniciarListaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerListaConItemsUseCase: ObtenerListaConItemsUseCase,
    private val agregarItemListaUseCase: AgregarItemListaUseCase,
    private val marcarItemListaUseCase: MarcarItemListaUseCase,
    private val eliminarItemListaUseCase: EliminarItemListaUseCase,
    private val reiniciarListaUseCase: ReiniciarListaUseCase,
    private val eliminarListaUseCase: EliminarListaUseCase,
) : ViewModel() {

    val listaId: String = checkNotNull(savedStateHandle["listaId"])

    private val _uiState = MutableStateFlow(ListDetailUiState())
    val uiState: StateFlow<ListDetailUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            obtenerListaConItemsUseCase(listaId).collect { listaConItems ->
                if (listaConItems == null) {
                    _uiState.update { it.copy(cargando = false, eliminada = true) }
                } else {
                    _uiState.update {
                        it.copy(cargando = false, nombre = listaConItems.nombre, items = listaConItems.items)
                    }
                }
            }
        }
    }

    fun agregarItem(texto: String) {
        viewModelScope.launch { agregarItemListaUseCase(listaId, texto, sesionActual().usuarioId) }
    }

    fun marcarItem(itemId: String, marcado: Boolean) {
        viewModelScope.launch { marcarItemListaUseCase(itemId, marcado, sesionActual().usuarioId) }
    }

    fun eliminarItem(itemId: String) {
        viewModelScope.launch { eliminarItemListaUseCase(itemId, sesionActual().usuarioId) }
    }

    fun reiniciar() {
        viewModelScope.launch { reiniciarListaUseCase(listaId, sesionActual().usuarioId) }
    }

    fun eliminarLista() {
        viewModelScope.launch { eliminarListaUseCase(listaId, sesionActual().usuarioId) }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
