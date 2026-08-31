package com.aqpseller.lulaapp.features.family

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.FrecuenciaRetoFamiliar
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.espacio.ObtenerMiembrosEspacioUseCase
import com.aqpseller.lulaapp.domain.usecase.retofamiliar.CrearRetoFamiliarUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MiembroSeleccionableUi(val usuarioId: String, val nombre: String, val seleccionado: Boolean)

/** El espacio en el que se crea el reto es el que trae la navegación (`espacioId`), no
 * necesariamente el espacio activo — mismo criterio que `RetosFamiliaresViewModel`. */
@HiltViewModel
class CrearRetoFamiliarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerMiembrosEspacioUseCase: ObtenerMiembrosEspacioUseCase,
    private val crearRetoFamiliarUseCase: CrearRetoFamiliarUseCase,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val espacioId: String = checkNotNull(savedStateHandle["espacioId"])
    private var sesion: SesionActual? = null

    private val _miembros = MutableStateFlow<List<MiembroSeleccionableUi>>(emptyList())
    val miembros: StateFlow<List<MiembroSeleccionableUi>> = _miembros.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    fun errorMostrado() {
        _mensajeError.value = null
    }

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            val nombreUsuario = usuarioRepository.observarUsuario().first()?.nombrePreferido ?: "Tú"
            obtenerMiembrosEspacioUseCase(espacioId).collect { miembros ->
                val seleccionActual = _miembros.value.associate { it.usuarioId to it.seleccionado }
                _miembros.value = miembros.map { miembro ->
                    MiembroSeleccionableUi(
                        usuarioId = miembro.usuarioId,
                        nombre = if (miembro.usuarioId == sesionActual.usuarioId) nombreUsuario else miembro.usuarioId,
                        // Todos empiezan marcados — hoy casi siempre es solo uno mismo.
                        seleccionado = seleccionActual[miembro.usuarioId] ?: true,
                    )
                }
            }
        }
    }

    fun alternarSeleccion(usuarioId: String) {
        _miembros.value = _miembros.value.map {
            if (it.usuarioId == usuarioId) it.copy(seleccionado = !it.seleccionado) else it
        }
    }

    fun guardar(nombre: String, objetivo: String, frecuencia: FrecuenciaRetoFamiliar, recompensa: String?) {
        if (nombre.isBlank()) {
            _mensajeError.value = "Escribe el nombre del reto"
            return
        }
        if (objetivo.isBlank()) {
            _mensajeError.value = "Escribe el objetivo del reto"
            return
        }
        val participantes = _miembros.value.filter { it.seleccionado }.map { it.usuarioId }
        viewModelScope.launch {
            val sesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
            crearRetoFamiliarUseCase(
                espacioId = espacioId,
                nombre = nombre,
                objetivo = objetivo,
                frecuencia = frecuencia,
                recompensa = recompensa?.takeIf { it.isNotBlank() },
                participantesIds = participantes,
                creadoPor = sesionActual.usuarioId,
            )
            _guardado.value = true
        }
    }
}
