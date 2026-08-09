package com.aqpseller.lulaapp.features.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.diario.ActualizarEntradaDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.diario.CrearEntradaDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.diario.EliminarEntradaDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.diario.ObtenerEntradaDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiaryEntryEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerEntradaDiarioUseCase: ObtenerEntradaDiarioUseCase,
    private val crearEntradaDiarioUseCase: CrearEntradaDiarioUseCase,
    private val actualizarEntradaDiarioUseCase: ActualizarEntradaDiarioUseCase,
    private val eliminarEntradaDiarioUseCase: EliminarEntradaDiarioUseCase,
) : ViewModel() {

    private val entradaId: String? = savedStateHandle.get<String>("entradaId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = entradaId != null

    /** Fecha con la que abrir una entrada nueva, cuando se llega desde un día puntual del calendario del Diario. */
    val fechaPreset: Long? = savedStateHandle.get<String>("fecha")?.toLongOrNull()

    private val _estadoInicial = MutableStateFlow<EntradaDiario?>(null)
    val estadoInicial: StateFlow<EntradaDiario?> = _estadoInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _eliminada = MutableStateFlow(false)
    val eliminada: StateFlow<Boolean> = _eliminada.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        val id = entradaId
        if (id != null) {
            viewModelScope.launch {
                obtenerEntradaDiarioUseCase(id)?.let { _estadoInicial.value = it }
            }
        }
    }

    fun guardar(texto: String, fecha: Long) {
        if (texto.isBlank()) {
            _mensajeError.value = "Escribe algo antes de guardar"
            return
        }
        viewModelScope.launch {
            val sesionActual = sesionActual()
            val id = entradaId
            if (id != null) {
                actualizarEntradaDiarioUseCase(id, sesionActual.usuarioId, null, texto, null, fecha)
            } else {
                crearEntradaDiarioUseCase(sesionActual.espacioPersonalId, sesionActual.usuarioId, null, texto, null, fecha)
            }
            _guardado.value = true
        }
    }

    fun eliminar() {
        val id = entradaId ?: return
        viewModelScope.launch {
            eliminarEntradaDiarioUseCase(id, sesionActual().usuarioId)
            _eliminada.value = true
        }
    }

    fun errorMostrado() {
        _mensajeError.value = null
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
