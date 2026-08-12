package com.aqpseller.lulaapp.features.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.CategoriaMeta
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHabitosUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.ActualizarMetaUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.CrearMetaUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.ObtenerDetalleMetaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearMetaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerHabitosUseCase: ObtenerHabitosUseCase,
    private val obtenerDetalleMetaUseCase: ObtenerDetalleMetaUseCase,
    private val crearMetaUseCase: CrearMetaUseCase,
    private val actualizarMetaUseCase: ActualizarMetaUseCase,
    private val espacioRepository: EspacioRepository,
) : ViewModel() {

    private val metaId: String? = savedStateHandle.get<String>("metaId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = metaId != null

    private val _habitosDisponibles = MutableStateFlow<List<Actividad>>(emptyList())
    val habitosDisponibles: StateFlow<List<Actividad>> = _habitosDisponibles.asStateFlow()

    private val _areasDisponibles = MutableStateFlow<List<AreaDeVida>>(emptyList())
    val areasDisponibles: StateFlow<List<AreaDeVida>> = _areasDisponibles.asStateFlow()

    private val _estadoInicial = MutableStateFlow<Meta?>(null)
    val estadoInicial: StateFlow<Meta?> = _estadoInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            obtenerHabitosUseCase(sesionActual.espacioId).collect { habitos -> _habitosDisponibles.value = habitos }
        }
        viewModelScope.launch {
            espacioRepository.observarAreasDeVidaActivas().collect { _areasDisponibles.value = it }
        }
        val id = metaId
        if (id != null) {
            viewModelScope.launch {
                obtenerDetalleMetaUseCase(id)?.let { _estadoInicial.value = it }
            }
        }
    }

    fun guardar(
        nombre: String,
        comoSeMide: ComoSeMideMeta,
        valorObjetivo: Double,
        actividadVinculadaId: String?,
        areaDeVidaId: String?,
        fechaLimite: Long?,
        categoria: CategoriaMeta? = null,
        avisarAlVencer: Boolean = false,
    ) {
        if (nombre.isBlank() || valorObjetivo <= 0) return
        viewModelScope.launch {
            val sesionActual = sesionActual()
            val id = metaId
            if (id != null) {
                val actual = _estadoInicial.value
                actualizarMetaUseCase(
                    metaId = id,
                    espacioId = sesionActual.espacioId,
                    usuarioId = sesionActual.usuarioId,
                    nombre = nombre,
                    comoSeMide = comoSeMide,
                    valorObjetivo = valorObjetivo,
                    valorActual = actual?.valorActual ?: 0.0,
                    actividadVinculadaId = actividadVinculadaId,
                    areaDeVidaId = areaDeVidaId,
                    fechaLimite = fechaLimite,
                    categoria = categoria,
                    avisarAlVencer = avisarAlVencer,
                )
            } else {
                crearMetaUseCase(
                    espacioId = sesionActual.espacioId,
                    usuarioId = sesionActual.usuarioId,
                    nombre = nombre,
                    comoSeMide = comoSeMide,
                    valorObjetivo = valorObjetivo,
                    actividadVinculadaId = actividadVinculadaId,
                    areaDeVidaId = areaDeVidaId,
                    fechaLimite = fechaLimite,
                    categoria = categoria,
                    avisarAlVencer = avisarAlVencer,
                )
            }
            _guardado.value = true
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
