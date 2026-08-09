package com.aqpseller.lulaapp.features.important_dates

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.TipoAviso
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.fechaimportante.ActualizarFechaImportanteUseCase
import com.aqpseller.lulaapp.domain.usecase.fechaimportante.CrearFechaImportanteUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FechaImportanteFormInicial(
    val nombre: String,
    val fechaBase: Long,
    val recurrencia: Recurrencia,
    val horaNotificacion: String,
    val anticipacion: AnticipacionRecordatorio,
    val tipoAviso: TipoAviso,
)

@HiltViewModel
class CrearFechaImportanteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val crearFechaImportanteUseCase: CrearFechaImportanteUseCase,
    private val actualizarFechaImportanteUseCase: ActualizarFechaImportanteUseCase,
) : ViewModel() {

    private val actividadId: String? = savedStateHandle.get<String>("actividadId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = actividadId != null

    private val _formInicial = MutableStateFlow<FechaImportanteFormInicial?>(null)
    val formInicial: StateFlow<FechaImportanteFormInicial?> = _formInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    init {
        val id = actividadId
        if (id != null) {
            viewModelScope.launch {
                val actividad = obtenerDetalleActividadUseCase(id)
                val detalle = actividad?.detalle as? ActividadDetalle.FechaImportante
                if (actividad != null && detalle != null) {
                    _formInicial.value = FechaImportanteFormInicial(
                        nombre = actividad.nombre,
                        fechaBase = detalle.fechaBase,
                        recurrencia = detalle.recurrencia,
                        horaNotificacion = detalle.horaNotificacion,
                        anticipacion = detalle.anticipacion,
                        tipoAviso = detalle.tipoAviso,
                    )
                }
            }
        }
    }

    fun guardar(
        nombre: String,
        fechaBase: Long,
        recurrencia: Recurrencia,
        horaNotificacion: String,
        anticipacion: AnticipacionRecordatorio,
        tipoAviso: TipoAviso,
    ) {
        if (nombre.isBlank()) {
            _mensajeError.value = "Escribe el nombre de la fecha importante"
            return
        }
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val id = actividadId
            if (id != null) {
                actualizarFechaImportanteUseCase(id, sesion.usuarioId, nombre, fechaBase, recurrencia, horaNotificacion, anticipacion, tipoAviso)
            } else {
                crearFechaImportanteUseCase(sesion.espacioId, sesion.usuarioId, nombre, fechaBase, recurrencia, horaNotificacion, anticipacion, tipoAviso)
            }
            _guardado.value = true
        }
    }

    fun errorMostrado() {
        _mensajeError.value = null
    }
}
