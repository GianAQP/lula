package com.aqpseller.lulaapp.features.health

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.notifications.AlarmaSonidoService
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
        // A propósito NO se detiene acá la Alarma si estaba sonando — mismo motivo que
        // `RecordatorioAccionViewModel`: esta pantalla también se abre sola por el
        // `fullScreenIntent`, sin acción real de la persona todavía. Se detiene recién en
        // `marcar`, cuando sí hay una acción real. Ver `08-decisiones-tecnicas.md`.
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
        detenerAlarmaSiSonando()
        viewModelScope.launch {
            marcarTomaMedicamentoUseCase(actividadId, horario, estado, sesionActual().usuarioId)
            _uiState.update { it.copy(estado = estado, listo = true) }
        }
    }

    /** "Ver en Mi salud" es la 3ra acción real de esta pantalla (además de "Ya la tomé"/"La
     * omito") — también debe cortar la Alarma si estaba sonando. Antes navegaba directo sin
     * pasar por acá, así que ese botón dejaba la Alarma sonando. */
    fun verEnMiSalud() {
        detenerAlarmaSiSonando()
        _uiState.update { it.copy(listo = true) }
    }

    private fun detenerAlarmaSiSonando() {
        context.startService(AlarmaSonidoService.intentDetener(context, "$actividadId:$horario".hashCode()))
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
