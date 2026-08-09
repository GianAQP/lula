package com.aqpseller.lulaapp.features.health

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.instruccionParaHorario
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.MarcarTomaMedicamentoUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.ObtenerTomasDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pantalla a la que manda tocar la notificación de una toma de Medicamento — igual que
 * `RecordatorioAccionScreen`, ofrece registrar el resultado sin obligar a pasar por el
 * formulario de edición.
 */
@HiltViewModel
class AccionTomaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val obtenerTomasDeHoyUseCase: ObtenerTomasDeHoyUseCase,
    private val marcarTomaMedicamentoUseCase: MarcarTomaMedicamentoUseCase,
) : ViewModel() {

    private val actividadId: String = checkNotNull(savedStateHandle["actividadId"])
    private val horario: String = checkNotNull(savedStateHandle["horario"])

    private val _uiState = MutableStateFlow(AccionTomaUiState())
    val uiState: StateFlow<AccionTomaUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        NotificationManagerCompat.from(context).cancel("$actividadId:$horario".hashCode())

        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            val actividad = obtenerDetalleActividadUseCase(actividadId)
            val detalle = actividad?.detalle as? ActividadDetalle.Medicamento
            if (actividad == null || detalle == null) {
                _uiState.update { it.copy(cargando = false, listo = true) }
                return@launch
            }
            val index = detalle.horariosCalculados.indexOf(horario).coerceAtLeast(0)
            val estadoActual = obtenerTomasDeHoyUseCase(listOf(actividadId)).first()
                .firstOrNull { it.horario == horario }
                ?.estado
                ?: EstadoActividad.SIN_CONFIRMAR
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombreMedicamento = actividad.nombre,
                    instruccion = instruccionParaHorario(detalle, index),
                    estado = estadoActual,
                )
            }
        }
    }

    fun marcar(estado: EstadoActividad) {
        viewModelScope.launch {
            marcarTomaMedicamentoUseCase(actividadId, horario, estado, sesionActual().usuarioId)
            _uiState.update { it.copy(estado = estado, listo = true) }
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
