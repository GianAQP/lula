package com.aqpseller.lulaapp.features.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.usecase.lista.CrearListaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearListaViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val crearListaUseCase: CrearListaUseCase,
) : ViewModel() {

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    fun errorMostrado() {
        _mensajeError.value = null
    }

    fun guardar(nombre: String, itemsTexto: List<String>) {
        if (nombre.isBlank()) {
            _mensajeError.value = "Escribe el nombre de la lista"
            return
        }
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            crearListaUseCase(sesion.espacioId, nombre, itemsTexto, sesion.usuarioId)
            _guardado.value = true
        }
    }
}
