package com.aqpseller.lulaapp.features.health

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.ui.CompartirPorQrController
import com.aqpseller.lulaapp.core.ui.CompartirQrEstado
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import com.aqpseller.lulaapp.domain.usecase.actividad.EliminarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerTareasVinculadasUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.PausarReanudarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.CompartirActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.ObtenerHistorialTomasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DIAS_HISTORIAL = 7

@HiltViewModel
class MedicamentoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val obtenerHistorialTomasUseCase: ObtenerHistorialTomasUseCase,
    private val obtenerTareasVinculadasUseCase: ObtenerTareasVinculadasUseCase,
    private val pausarReanudarActividadUseCase: PausarReanudarActividadUseCase,
    private val eliminarActividadUseCase: EliminarActividadUseCase,
    private val compartirActividadUseCase: CompartirActividadUseCase,
    private val compartirPorQrController: CompartirPorQrController,
) : ViewModel() {

    val actividadId: String = checkNotNull(savedStateHandle["actividadId"])

    private val _uiState = MutableStateFlow(MedicamentoDetailUiState())
    val uiState: StateFlow<MedicamentoDetailUiState> = _uiState.asStateFlow()

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
            val detalle = actividad.detalle as? ActividadDetalle.Medicamento
            val historial = obtenerHistorialTomasUseCase(actividadId, DIAS_HISTORIAL)
            val tareasVinculadas = obtenerTareasVinculadasUseCase(actividadId)
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = actividad.nombre,
                    dosis = detalle?.dosis.orEmpty(),
                    activa = actividad.activa,
                    historial = historial
                        .sortedWith(compareByDescending<TomaMedicamento> { it.fecha }.thenBy { it.horario })
                        .map { toma ->
                            HistorialTomaUi(
                                fechaTexto = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochDaysToLocalDate(toma.fecha)),
                                horario = toma.horario,
                                estado = toma.estado,
                            )
                        },
                    nombresTareasVinculadas = tareasVinculadas.map { it.nombre },
                )
            }
        }
    }

    fun pausarOReanudar() {
        viewModelScope.launch {
            pausarReanudarActividadUseCase(actividadId, !_uiState.value.activa, sesionActual().usuarioId)
            _uiState.update { it.copy(activa = !it.activa) }
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
            compartirPorQrController.generar(viewModelScope, usuarioId, actividadId, TipoActividad.MEDICAMENTO, _uiState.value.nombre, permiso)
        }
    }

    fun ocultarCodigoQr() = compartirPorQrController.ocultar()

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
