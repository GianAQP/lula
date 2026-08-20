package com.aqpseller.lulaapp.features.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea
import com.aqpseller.lulaapp.domain.usecase.actividad.ActualizarTareaUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.CrearTareaUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.ObtenerCitasUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.ObtenerMedicamentosUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TareaFormInicial(
    val nombre: String,
    val fechaLimite: Long?,
    val importante: Boolean,
    val urgente: Boolean,
    val horaRecordatorio: String?,
    val nivelRecordatorio: NivelRecordatorio,
    val recurrencia: RecurrenciaTarea,
    val actividadVinculadaId: String?,
)

/** Un Medicamento o Cita elegible para vincular una Tarea (ver `08-decisiones-tecnicas.md`). */
data class ActividadVinculableUi(val id: String, val nombre: String, val emoji: String)

@HiltViewModel
class CrearTareaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val crearTareaUseCase: CrearTareaUseCase,
    private val actualizarTareaUseCase: ActualizarTareaUseCase,
    private val obtenerMedicamentosUseCase: ObtenerMedicamentosUseCase,
    private val obtenerCitasUseCase: ObtenerCitasUseCase,
) : ViewModel() {

    private val actividadId: String? = savedStateHandle.get<String>("actividadId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = actividadId != null

    private val _formInicial = MutableStateFlow<TareaFormInicial?>(null)
    val formInicial: StateFlow<TareaFormInicial?> = _formInicial.asStateFlow()

    private val _actividadesVinculables = MutableStateFlow<List<ActividadVinculableUi>>(emptyList())
    val actividadesVinculables: StateFlow<List<ActividadVinculableUi>> = _actividadesVinculables.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    fun errorMostrado() {
        _mensajeError.value = null
    }

    init {
        val id = actividadId
        if (id != null) {
            viewModelScope.launch {
                val actividad = obtenerDetalleActividadUseCase(id)
                val detalle = actividad?.detalle as? ActividadDetalle.Tarea
                if (actividad != null && detalle != null) {
                    _formInicial.value = TareaFormInicial(
                        nombre = actividad.nombre,
                        fechaLimite = detalle.fechaLimite,
                        importante = detalle.importante,
                        urgente = detalle.urgente,
                        horaRecordatorio = detalle.horaRecordatorio,
                        nivelRecordatorio = detalle.nivelRecordatorio,
                        recurrencia = detalle.recurrencia,
                        actividadVinculadaId = detalle.actividadVinculadaId,
                    )
                }
            }
        }
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            combine(
                obtenerMedicamentosUseCase(sesion.espacioId),
                obtenerCitasUseCase(sesion.espacioId),
            ) { medicamentos, citas ->
                medicamentos.map { ActividadVinculableUi(it.id, it.nombre, "💊") } +
                    citas.map { ActividadVinculableUi(it.id, it.nombre, "🗓️") }
            }.collect { _actividadesVinculables.value = it }
        }
    }

    fun guardar(
        nombre: String,
        fechaLimite: Long?,
        importante: Boolean,
        urgente: Boolean,
        horaRecordatorio: String?,
        nivelRecordatorio: NivelRecordatorio,
        recurrencia: RecurrenciaTarea,
        actividadVinculadaId: String?,
    ) {
        // Antes esto se salía en silencio, sin avisar nada — el usuario tocaba "Crear" y no
        // pasaba nada visible, sin saber que le faltaba el nombre. A pedido del usuario. Ver
        // `Plan/08-decisiones-tecnicas.md`.
        if (nombre.isBlank()) {
            _mensajeError.value = "Falta el nombre de la tarea"
            return
        }
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val id = actividadId
            if (id != null) {
                actualizarTareaUseCase(
                    id, sesion.usuarioId, nombre, fechaLimite, importante, urgente,
                    horaRecordatorio, nivelRecordatorio, recurrencia, actividadVinculadaId,
                )
            } else {
                crearTareaUseCase(
                    espacioId = sesion.espacioId,
                    propietario = sesion.usuarioId,
                    nombre = nombre,
                    fechaLimite = fechaLimite,
                    importante = importante,
                    urgente = urgente,
                    horaRecordatorio = horaRecordatorio,
                    nivelRecordatorio = nivelRecordatorio,
                    recurrencia = recurrencia,
                    actividadVinculadaId = actividadVinculadaId,
                )
            }
            _guardado.value = true
        }
    }
}
