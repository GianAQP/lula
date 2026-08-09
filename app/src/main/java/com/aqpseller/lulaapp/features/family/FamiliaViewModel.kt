package com.aqpseller.lulaapp.features.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.espacio.CambiarEspacioActivoUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.CrearEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.EliminarEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.ObtenerEspaciosDeUsuarioUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.ObtenerMiembrosEspacioUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.RenombrarEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Familia / Espacios" (CONFIGURACIÓN en el menú "⋮") — crear el espacio Familia, ver quién es
 * miembro y cambiar el espacio activo. Solo un espacio Familia por usuario en esta pasada, ver
 * `Plan/08-decisiones-tecnicas.md` (base local de Fase 1.5, sin invitaciones reales todavía).
 */
@HiltViewModel
class FamiliaViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerEspaciosDeUsuarioUseCase: ObtenerEspaciosDeUsuarioUseCase,
    private val obtenerMiembrosEspacioUseCase: ObtenerMiembrosEspacioUseCase,
    private val crearEspacioFamiliaUseCase: CrearEspacioFamiliaUseCase,
    private val cambiarEspacioActivoUseCase: CambiarEspacioActivoUseCase,
    private val renombrarEspacioFamiliaUseCase: RenombrarEspacioFamiliaUseCase,
    private val eliminarEspacioFamiliaUseCase: EliminarEspacioFamiliaUseCase,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamiliaUiState())
    val uiState: StateFlow<FamiliaUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null
    private var nombreUsuarioActual: String = "Tú"

    init {
        viewModelScope.launch {
            usuarioRepository.observarUsuario().collect { usuario ->
                nombreUsuarioActual = usuario?.nombrePreferido ?: "Tú"
            }
        }
        viewModelScope.launch {
            val sesionActual = sesionActual()
            obtenerEspaciosDeUsuarioUseCase(sesionActual.usuarioId).collectLatest { espacios ->
                val familia = espacios.firstOrNull { it.tipo == TipoEspacio.FAMILIA }
                _uiState.update { estado ->
                    estado.copy(
                        cargando = false,
                        espacios = espacios.map { espacio ->
                            EspacioUi(
                                id = espacio.id,
                                nombre = espacio.nombre,
                                esFamilia = espacio.tipo == TipoEspacio.FAMILIA,
                                esActivo = espacio.id == sesionActual.espacioId,
                            )
                        },
                        espacioFamiliaId = familia?.id,
                        nombreEspacioFamilia = familia?.nombre.orEmpty(),
                    )
                }
                if (familia != null) cargarMiembros(familia.id)
            }
        }
    }

    private fun cargarMiembros(espacioId: String) {
        viewModelScope.launch {
            obtenerMiembrosEspacioUseCase(espacioId).collect { miembros ->
                _uiState.update { estado ->
                    estado.copy(
                        miembros = miembros.map { miembro ->
                            val nombre = if (miembro.usuarioId == sesion?.usuarioId) nombreUsuarioActual else miembro.usuarioId
                            MiembroUi(nombre = nombre, rol = if (miembro.rol == RolEnEspacio.ADMIN) "Admin" else "Miembro")
                        },
                    )
                }
            }
        }
    }

    fun mostrarFormularioCrear() {
        _uiState.update { it.copy(mostrarFormularioCrear = true) }
    }

    fun ocultarFormularioCrear() {
        _uiState.update { it.copy(mostrarFormularioCrear = false) }
    }

    fun crearEspacioFamilia(nombre: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            crearEspacioFamiliaUseCase(nombre, sesionActual().usuarioId)
            _uiState.update { it.copy(mostrarFormularioCrear = false, espacioCambiado = true) }
        }
    }

    fun seleccionarEspacio(espacioId: String) {
        viewModelScope.launch {
            cambiarEspacioActivoUseCase(espacioId)
            _uiState.update { it.copy(espacioCambiado = true) }
        }
    }

    fun mostrarFormularioRenombrar() {
        _uiState.update { it.copy(mostrarFormularioRenombrar = true) }
    }

    fun ocultarFormularioRenombrar() {
        _uiState.update { it.copy(mostrarFormularioRenombrar = false) }
    }

    fun renombrarEspacioFamilia(nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        val espacioId = _uiState.value.espacioFamiliaId ?: return
        viewModelScope.launch {
            renombrarEspacioFamiliaUseCase(espacioId, nuevoNombre, sesionActual().usuarioId)
            _uiState.update { it.copy(mostrarFormularioRenombrar = false) }
        }
    }

    fun eliminarEspacioFamilia() {
        val espacioId = _uiState.value.espacioFamiliaId ?: return
        viewModelScope.launch {
            eliminarEspacioFamiliaUseCase(espacioId, sesionActual().usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
