package com.aqpseller.lulaapp.features.health

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.ui.CompartirPorQrController
import com.aqpseller.lulaapp.core.ui.CompartirQrEstado
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.usecase.actividad.EliminarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.MarcarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerTareasVinculadasUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.CompartirActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.MarcarSesionCitaUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.ObtenerSesionesCitaUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.ReprogramarSesionCitaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CitaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val obtenerTareasVinculadasUseCase: ObtenerTareasVinculadasUseCase,
    private val eliminarActividadUseCase: EliminarActividadUseCase,
    private val compartirActividadUseCase: CompartirActividadUseCase,
    private val marcarActividadUseCase: MarcarActividadUseCase,
    private val obtenerSesionesCitaUseCase: ObtenerSesionesCitaUseCase,
    private val marcarSesionCitaUseCase: MarcarSesionCitaUseCase,
    private val reprogramarSesionCitaUseCase: ReprogramarSesionCitaUseCase,
    private val compartirPorQrController: CompartirPorQrController,
) : ViewModel() {

    val actividadId: String = checkNotNull(savedStateHandle["actividadId"])

    private val _uiState = MutableStateFlow(CitaDetailUiState())
    val uiState: StateFlow<CitaDetailUiState> = _uiState.asStateFlow()

    private val _solicitudEnviada = MutableStateFlow(false)
    val solicitudEnviada: StateFlow<Boolean> = _solicitudEnviada.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        cargar()
    }

    /**
     * Navigation Compose reutiliza esta misma instancia del ViewModel al volver de Editar
     * (la pantalla no se recrea, solo se recompone) — sin volver a llamar `cargar()`, el
     * `uiState` se quedaba con la foto de antes de editar hasta salir y volver a entrar.
     */
    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            val actividad = obtenerDetalleActividadUseCase(actividadId) ?: return@launch
            val detalle = actividad.detalle as? ActividadDetalle.Cita
            val tareasVinculadas = obtenerTareasVinculadasUseCase(actividadId)
            val sesiones = if (detalle?.esCurso == true) {
                obtenerSesionesCitaUseCase(actividadId).map {
                    SesionCitaUi(numeroSesion = it.numeroSesion, fecha = it.fecha, horario = it.horario, estado = it.estado)
                }
            } else {
                emptyList()
            }
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = actividad.nombre,
                    motivo = detalle?.motivo,
                    lugar = detalle?.lugar,
                    fechaHora = detalle?.fechaHora ?: 0L,
                    estado = actividad.estado,
                    nombresTareasVinculadas = tareasVinculadas.map { it.nombre },
                    esCurso = detalle?.esCurso ?: false,
                    sesiones = sesiones,
                    cantidadSesionesTotal = detalle?.cantidadSesionesTotal,
                )
            }
        }
    }

    fun marcarSesion(numeroSesion: Int, estado: EstadoActividad) {
        viewModelScope.launch {
            marcarSesionCitaUseCase(actividadId, numeroSesion, estado, sesionActual().usuarioId)
            _uiState.update { estado_ ->
                estado_.copy(
                    sesiones = estado_.sesiones.map { if (it.numeroSesion == numeroSesion) it.copy(estado = estado) else it },
                )
            }
        }
    }

    fun reprogramarSesion(numeroSesion: Int, nuevaFechaEpochDay: Long) {
        viewModelScope.launch {
            reprogramarSesionCitaUseCase(actividadId, numeroSesion, nuevaFechaEpochDay, sesionActual().usuarioId)
            _uiState.update { estado ->
                estado.copy(
                    sesiones = estado.sesiones.map { if (it.numeroSesion == numeroSesion) it.copy(fecha = nuevaFechaEpochDay) else it },
                )
            }
        }
    }

    fun marcarCumplida() {
        viewModelScope.launch {
            marcarActividadUseCase(actividadId, EstadoActividad.CONFIRMADO, sesionActual().usuarioId)
            _uiState.update { it.copy(estado = EstadoActividad.CONFIRMADO) }
        }
    }

    /** "No fui" / se postergó — mismo estado que usa Hábito para "omitido", nunca se trata como
     * un fallo, solo dice que esta vez no pasó (ver `Plan/CLAUDE.md`). */
    fun marcarOmitida() {
        viewModelScope.launch {
            marcarActividadUseCase(actividadId, EstadoActividad.OMITIDO, sesionActual().usuarioId)
            _uiState.update { it.copy(estado = EstadoActividad.OMITIDO) }
        }
    }

    fun eliminar() {
        viewModelScope.launch {
            eliminarActividadUseCase(actividadId, sesionActual().usuarioId)
            _uiState.update { it.copy(eliminado = true) }
        }
    }

    fun compartir(contacto: String, permiso: PermisoCompartir) {
        viewModelScope.launch {
            compartirActividadUseCase(sesionActual().usuarioId, actividadId, _uiState.value.nombre, contacto, permiso)
            _solicitudEnviada.value = true
        }
    }

    fun solicitudEnviadaMostrada() {
        _solicitudEnviada.value = false
    }

    val estadoCompartirQr: StateFlow<CompartirQrEstado> = compartirPorQrController.estado

    fun generarCodigoQr(permiso: PermisoCompartir) {
        viewModelScope.launch {
            val usuarioId = sesionActual().usuarioId
            compartirPorQrController.generar(viewModelScope, usuarioId, actividadId, TipoActividad.CITA, _uiState.value.nombre, permiso)
        }
    }

    fun ocultarCodigoQr() = compartirPorQrController.ocultar()

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
