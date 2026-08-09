package com.aqpseller.lulaapp.features.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerHistorialDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Tu semana" — el resumen que `02-pantallas.md` describe como cabecera de Progreso, antes del
 * link a la Revisión semanal completa. Mismo cálculo de cumplimiento/racha máxima semanal que
 * `WeeklyReviewViewModel` (ventana de la semana ISO en curso), más Constancia de 30 días (la
 * misma fórmula que ya usa Historial) y Puntos esta semana (nuevo: suma de `puntuacion` de los
 * días cerrados de la semana). A propósito no repite el formulario de reflexión/guardar — ese
 * sigue viviendo solo en Revisión semanal, un tap más allá.
 */
@HiltViewModel
class ProgresoViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerHistorialDiarioUseCase: ObtenerHistorialDiarioUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgresoUiState())
    val uiState: StateFlow<ProgresoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()

            val inicioSemana = DateTimeUtils.inicioDeSemanaEpochDias()
            val finSemana = DateTimeUtils.finDeSemanaEpochDias()
            val registrosSemana = obtenerHistorialDiarioUseCase(sesion.espacioId).first()
                .filter { it.fecha in inicioSemana..finSemana }
                .associateBy { it.fecha }

            val totalCompletadas = registrosSemana.values.sumOf { it.actividadesCompletadas }
            val totalActividades = registrosSemana.values.sumOf { it.actividadesTotales }
            val cumplimiento = if (totalActividades > 0) totalCompletadas * 100 / totalActividades else 0
            val puntosSemana = registrosSemana.values.sumOf { it.puntuacion }

            var rachaMaxima = 0
            var rachaEnRecorrido = 0
            for (fecha in inicioSemana..finSemana) {
                val registro = registrosSemana[fecha]
                if (registro != null && registro.actividadesCompletadas > 0) {
                    rachaEnRecorrido++
                    rachaMaxima = maxOf(rachaMaxima, rachaEnRecorrido)
                } else {
                    rachaEnRecorrido = 0
                }
            }

            val constancia = obtenerProgresoDeHoyUseCase.calcularConstancia(sesion.espacioId)

            _uiState.update {
                it.copy(
                    cargando = false,
                    cumplimientoSemana = cumplimiento,
                    rachaMaximaSemana = rachaMaxima,
                    constancia30Dias = constancia,
                    puntosSemana = puntosSemana,
                )
            }
        }
    }
}
