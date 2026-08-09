package com.aqpseller.lulaapp.features.family

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

@HiltViewModel
class CrearRetoFamiliarViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerMiembrosEspacioUseCase: ObtenerMiembrosEspacioUseCase,
    private val crearRetoFamiliarUseCase: CrearRetoFamiliarUseCase,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private var sesion: SesionActual? = null

    private val _miembros = MutableStateFlow<List<MiembroSeleccionableUi>>(emptyList())
    val miembros: StateFlow<List<MiembroSeleccionableUi>> = _miembros.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            val nombreUsuario = usuarioRepository.observarUsuario().first()?.nombrePreferido ?: "Tú"
            obtenerMiembrosEspacioUseCase(sesionActual.espacioId).collect { miembros ->
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
        if (nombre.isBlank() || objetivo.isBlank()) return
        val participantes = _miembros.value.filter { it.seleccionado }.map { it.usuarioId }
        viewModelScope.launch {
            val sesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
            crearRetoFamiliarUseCase(
                espacioId = sesionActual.espacioId,
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
