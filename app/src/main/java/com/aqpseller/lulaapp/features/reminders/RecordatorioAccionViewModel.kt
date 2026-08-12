package com.aqpseller.lulaapp.features.reminders

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.notifications.AlarmaSonidoService
import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.usecase.actividad.MarcarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pantalla a la que manda tocar una notificación de recordatorio — a propósito NO es la
 * pantalla de editar (era la fuente de confusión que reportó el usuario): ofrece marcar hecho
 * o posponer, sin obligar a pasar por el formulario.
 */
@HiltViewModel
class RecordatorioAccionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val marcarActividadUseCase: MarcarActividadUseCase,
    private val recordatorioScheduler: RecordatorioScheduler,
) : ViewModel() {

    private val actividadId: String = checkNotNull(savedStateHandle["actividadId"])

    private val _uiState = MutableStateFlow(RecordatorioAccionUiState())
    val uiState: StateFlow<RecordatorioAccionUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null
    private var horaRecordatorio: String? = null
    private var nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO

    init {
        // A propósito NO se detiene la Alarma acá — el `fullScreenIntent` abre esta pantalla
        // SOLO, sin que la persona haga nada, así que detener acá era el mismo bug que se acaba
        // de corregir (la app se apagaba la propia alarma apenas se abría la pantalla, antes de
        // que la persona llegara a verla). La Alarma se detiene recién cuando la persona toca
        // una acción real de acá abajo — ver `detenerAlarmaSiSonando`, `08-decisiones-tecnicas.md`.
        viewModelScope.launch {
            sesion = obtenerSesionActualUseCase()
            val actividad = obtenerDetalleActividadUseCase(actividadId)
            if (actividad == null) {
                _uiState.update { it.copy(cargando = false, listo = true) }
                return@launch
            }
            when (val detalle = actividad.detalle) {
                is ActividadDetalle.Habito -> {
                    horaRecordatorio = detalle.horaRecordatorio
                    nivelRecordatorio = detalle.nivelRecordatorio
                }
                is ActividadDetalle.Tarea -> {
                    horaRecordatorio = detalle.horaRecordatorio
                    nivelRecordatorio = detalle.nivelRecordatorio
                }
                else -> Unit
            }
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = actividad.nombre,
                    esHabito = actividad.tipo == TipoActividad.HABITO,
                )
            }
        }
    }

    fun marcarHecho() {
        detenerAlarmaSiSonando()
        viewModelScope.launch {
            marcarActividadUseCase(actividadId, EstadoActividad.CONFIRMADO, sesionActual().usuarioId)
            _uiState.update { it.copy(listo = true) }
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }

    fun posponer(minutos: Int = 15) {
        detenerAlarmaSiSonando()
        val hora = horaRecordatorio ?: run { _uiState.update { it.copy(listo = true) }; return }
        recordatorioScheduler.posponer(actividadId, _uiState.value.nombre, _uiState.value.esHabito, hora, nivelRecordatorio, minutos)
        _uiState.update { it.copy(listo = true) }
    }

    fun irAHoy() {
        detenerAlarmaSiSonando()
        _uiState.update { it.copy(listo = true) }
    }

    /** Tocar cualquiera de las 3 acciones de esta pantalla SÍ es una acción real de la persona
     * (a diferencia de que Android abra la pantalla sola por el `fullScreenIntent`) — ahí
     * corresponde cortar la Alarma si estaba sonando. Cancela también la notificación de
     * recordatorio; no pasa nada si no había ninguna Alarma sonando (`AlarmaSonidoService` no
     * hace nada si no tiene nada que detener). */
    private fun detenerAlarmaSiSonando() {
        context.startService(AlarmaSonidoService.intentDetener(context, actividadId.hashCode()))
    }
}
