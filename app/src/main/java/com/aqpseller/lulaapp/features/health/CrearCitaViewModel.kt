package com.aqpseller.lulaapp.features.health

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecordatorioCita
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.ActualizarCitaUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.CrearCitaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CitaFormInicial(
    val nombre: String,
    val lugar: String?,
    val motivo: String?,
    val fechaHora: Long,
    val recordatorios: List<RecordatorioCita>,
    val nivelRecordatorio: NivelRecordatorio,
    val esCurso: Boolean,
    val diasSemana: Set<Int>,
    val horaSesion: String?,
    val fechaInicioCurso: Long?,
    val cantidadSesionesTotal: Int?,
)

@HiltViewModel
class CrearCitaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val crearCitaUseCase: CrearCitaUseCase,
    private val actualizarCitaUseCase: ActualizarCitaUseCase,
) : ViewModel() {

    private val actividadId: String? = savedStateHandle.get<String>("actividadId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = actividadId != null

    private val _formInicial = MutableStateFlow<CitaFormInicial?>(null)
    val formInicial: StateFlow<CitaFormInicial?> = _formInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    init {
        val id = actividadId
        if (id != null) {
            viewModelScope.launch {
                val actividad = obtenerDetalleActividadUseCase(id)
                val detalle = actividad?.detalle as? ActividadDetalle.Cita
                if (actividad != null && detalle != null) {
                    _formInicial.value = CitaFormInicial(
                        nombre = actividad.nombre,
                        lugar = detalle.lugar,
                        motivo = detalle.motivo,
                        fechaHora = detalle.fechaHora,
                        recordatorios = detalle.recordatorios,
                        nivelRecordatorio = detalle.nivelRecordatorio,
                        esCurso = detalle.esCurso,
                        diasSemana = detalle.diasSemana,
                        horaSesion = detalle.horaSesion,
                        fechaInicioCurso = detalle.fechaInicioCurso,
                        cantidadSesionesTotal = detalle.cantidadSesionesTotal,
                    )
                }
            }
        }
    }

    fun guardar(
        nombre: String,
        lugar: String?,
        motivo: String?,
        fechaHora: Long,
        recordatorios: List<RecordatorioCita>,
        nivelRecordatorio: NivelRecordatorio,
        esCurso: Boolean = false,
        diasSemana: Set<Int> = emptySet(),
        horaSesion: String? = null,
        fechaInicioCurso: Long? = null,
        cantidadSesionesTotal: Int? = null,
    ) {
        if (nombre.isBlank()) {
            _mensajeError.value = "Escribe el nombre de la cita"
            return
        }
        if (esCurso && diasSemana.isEmpty()) {
            _mensajeError.value = "Elige al menos un día de la semana para el curso"
            return
        }
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val id = actividadId
            if (id != null) {
                actualizarCitaUseCase(
                    id, sesion.usuarioId, nombre, lugar, motivo, fechaHora, recordatorios, nivelRecordatorio,
                    esCurso, diasSemana, horaSesion, fechaInicioCurso, cantidadSesionesTotal,
                )
            } else {
                crearCitaUseCase(
                    espacioId = sesion.espacioId,
                    propietario = sesion.usuarioId,
                    nombre = nombre,
                    lugar = lugar,
                    motivo = motivo,
                    fechaHora = fechaHora,
                    recordatorios = recordatorios,
                    nivelRecordatorio = nivelRecordatorio,
                    esCurso = esCurso,
                    diasSemana = diasSemana,
                    horaSesion = horaSesion,
                    fechaInicioCurso = fechaInicioCurso,
                    cantidadSesionesTotal = cantidadSesionesTotal,
                )
            }
            _guardado.value = true
        }
    }

    fun errorMostrado() {
        _mensajeError.value = null
    }
}
