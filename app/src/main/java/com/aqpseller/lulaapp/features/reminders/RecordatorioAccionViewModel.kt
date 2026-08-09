package com.aqpseller.lulaapp.features.reminders

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        // Al abrirse esta pantalla (por toque o por el fullScreenIntent de Alarma), la
        // notificación ya cumplió su función — cancelarla apaga su sonido/vibración. Sin esto
        // el sonido seguía sonando aunque el usuario ya hubiera tocado una acción, porque el
        // fullScreenIntent no pasa por el flujo normal de "tocar la notificación" que dispara
        // `setAutoCancel`.
        NotificationManagerCompat.from(context).cancel(actividadId.hashCode())

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
        viewModelScope.launch {
            marcarActividadUseCase(actividadId, EstadoActividad.CONFIRMADO, sesionActual().usuarioId)
            _uiState.update { it.copy(listo = true) }
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }

    fun posponer(minutos: Int = 15) {
        val hora = horaRecordatorio ?: run { _uiState.update { it.copy(listo = true) }; return }
        recordatorioScheduler.posponer(actividadId, _uiState.value.nombre, _uiState.value.esHabito, hora, nivelRecordatorio, minutos)
        _uiState.update { it.copy(listo = true) }
    }

    fun irAHoy() {
        _uiState.update { it.copy(listo = true) }
    }
}
