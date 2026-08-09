package com.aqpseller.lulaapp.features.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ListaEjecucion
import com.aqpseller.lulaapp.domain.usecase.lista.ObtenerHistorialListaUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.ObtenerListaConItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun ListaEjecucion.aItemUi() = ListaEjecucionItemUi(
    id = id,
    fechaTexto = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fecha)),
    marcados = marcados,
    total = total,
    items = items,
)

@HiltViewModel
class ListHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerListaConItemsUseCase: ObtenerListaConItemsUseCase,
    private val obtenerHistorialListaUseCase: ObtenerHistorialListaUseCase,
) : ViewModel() {

    val listaId: String = checkNotNull(savedStateHandle["listaId"])

    private val _uiState = MutableStateFlow(ListHistoryUiState())
    val uiState: StateFlow<ListHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val nombreLista = obtenerListaConItemsUseCase(listaId).first()?.nombre.orEmpty()
            obtenerHistorialListaUseCase(listaId).collect { ejecuciones ->
                _uiState.value = ListHistoryUiState(
                    cargando = false,
                    nombreLista = nombreLista,
                    ejecuciones = ejecuciones.map { it.aItemUi() },
                )
            }
        }
    }
}
