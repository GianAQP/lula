package com.aqpseller.lulaapp.features.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.FrecuenciaHabito
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.usecase.actividad.ActualizarHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.CrearHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitoFormInicial(
    val nombre: String,
    val momentoDelDia: MomentoDelDia,
    val duracionInicialMin: Int?,
    val horaRecordatorio: String?,
    val nivelRecordatorio: NivelRecordatorio,
    val duracionObjetivoMin: Int?,
    val incrementoMin: Int?,
    val frecuenciaRevisionDias: Int?,
)

@HiltViewModel
class CrearHabitoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val crearHabitoUseCase: CrearHabitoUseCase,
    private val actualizarHabitoUseCase: ActualizarHabitoUseCase,
) : ViewModel() {

    private val actividadId: String? = savedStateHandle.get<String>("actividadId")?.takeIf { it.isNotBlank() }
    val esEdicion: Boolean get() = actividadId != null

    private val _formInicial = MutableStateFlow<HabitoFormInicial?>(null)
    val formInicial: StateFlow<HabitoFormInicial?> = _formInicial.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    init {
        val id = actividadId
        if (id != null) {
            viewModelScope.launch {
                val actividad = obtenerDetalleActividadUseCase(id)
                val detalle = actividad?.detalle as? ActividadDetalle.Habito
                if (actividad != null && detalle != null) {
                    _formInicial.value = HabitoFormInicial(
                        nombre = actividad.nombre,
                        momentoDelDia = detalle.momentoDelDia,
                        duracionInicialMin = detalle.duracionInicialMin,
                        horaRecordatorio = detalle.horaRecordatorio,
                        nivelRecordatorio = detalle.nivelRecordatorio,
                        duracionObjetivoMin = detalle.duracionObjetivoMin,
                        incrementoMin = detalle.incrementoMin,
                        frecuenciaRevisionDias = detalle.frecuenciaRevisionDias,
                    )
                }
            }
        }
    }

    fun guardar(
        nombre: String,
        momentoDelDia: MomentoDelDia,
        duracionInicialMin: Int?,
        horaRecordatorio: String?,
        nivelRecordatorio: NivelRecordatorio,
        duracionObjetivoMin: Int?,
        incrementoMin: Int?,
        frecuenciaRevisionDias: Int?,
    ) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val id = actividadId
            if (id != null) {
                actualizarHabitoUseCase(
                    id, sesion.usuarioId, nombre, momentoDelDia, duracionInicialMin, horaRecordatorio, nivelRecordatorio,
                    duracionObjetivoMin, incrementoMin, frecuenciaRevisionDias,
                )
            } else {
                crearHabitoUseCase(
                    espacioId = sesion.espacioId,
                    propietario = sesion.usuarioId,
                    nombre = nombre,
                    momentoDelDia = momentoDelDia,
                    frecuencia = FrecuenciaHabito.DIARIA,
                    duracionInicialMin = duracionInicialMin,
                    horaRecordatorio = horaRecordatorio,
                    nivelRecordatorio = nivelRecordatorio,
                    duracionObjetivoMin = duracionObjetivoMin,
                    incrementoMin = incrementoMin,
                    frecuenciaRevisionDias = frecuenciaRevisionDias,
                )
            }
            _guardado.value = true
        }
    }
}
