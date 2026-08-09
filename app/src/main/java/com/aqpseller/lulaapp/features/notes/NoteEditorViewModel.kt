package com.aqpseller.lulaapp.features.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.nota.ActualizarNotaUseCase
import com.aqpseller.lulaapp.domain.usecase.nota.CrearNotaUseCase
import com.aqpseller.lulaapp.domain.usecase.nota.EliminarNotaUseCase
import com.aqpseller.lulaapp.domain.usecase.nota.ObtenerNotaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotaFormInicial(val titulo: String?, val contenido: String)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerNotaUseCase: ObtenerNotaUseCase,
    private val crearNotaUseCase: CrearNotaUseCase,
    private val actualizarNotaUseCase: ActualizarNotaUseCase,
    private val eliminarNotaUseCase: EliminarNotaUseCase,
) : ViewModel() {

    private val notaId: String? = savedStateHandle.get<String>("notaId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = notaId != null

    private val _formInicial = MutableStateFlow<NotaFormInicial?>(null)
    val formInicial: StateFlow<NotaFormInicial?> = _formInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _eliminada = MutableStateFlow(false)
    val eliminada: StateFlow<Boolean> = _eliminada.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        val id = notaId
        if (id != null) {
            viewModelScope.launch {
                obtenerNotaUseCase(id)?.let { _formInicial.value = NotaFormInicial(it.titulo, it.contenido) }
            }
        }
    }

    fun guardar(titulo: String, contenido: String) {
        if (contenido.isBlank()) {
            _mensajeError.value = "Escribe algo antes de guardar"
            return
        }
        viewModelScope.launch {
            val sesionActual = sesionActual()
            val id = notaId
            if (id != null) {
                actualizarNotaUseCase(id, sesionActual.usuarioId, titulo, contenido)
            } else {
                crearNotaUseCase(sesionActual.espacioPersonalId, sesionActual.usuarioId, titulo, contenido)
            }
            _guardado.value = true
        }
    }

    fun eliminar() {
        val id = notaId ?: return
        viewModelScope.launch {
            eliminarNotaUseCase(id, sesionActual().usuarioId)
            _eliminada.value = true
        }
    }

    fun errorMostrado() {
        _mensajeError.value = null
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
