package com.aqpseller.lulaapp.features.daily_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerActividadesDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.CerrarDiaUseCase
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

@HiltViewModel
class CerrarDiaViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerActividadesDeHoyUseCase: ObtenerActividadesDeHoyUseCase,
    private val cerrarDiaUseCase: CerrarDiaUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CerrarDiaUiState())
    val uiState: StateFlow<CerrarDiaUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            val actividades = obtenerActividadesDeHoyUseCase(sesionActual.espacioId).first()
            _uiState.update {
                it.copy(
                    cargando = false,
                    actividadesCompletadas = actividades.count { a -> a.estado == EstadoActividad.CONFIRMADO },
                    actividadesTotales = actividades.size,
                )
            }
        }
    }

    fun cerrarDia(queLogre: String?, queCosto: String?, queAjusto: String?) {
        val sesionActual = sesion ?: return
        viewModelScope.launch {
            val estado = _uiState.value
            cerrarDiaUseCase(
                espacioId = sesionActual.espacioId,
                usuarioId = sesionActual.usuarioId,
                actividadesCompletadas = estado.actividadesCompletadas,
                actividadesTotales = estado.actividadesTotales,
                queLogre = queLogre,
                queCosto = queCosto,
                queAjusto = queAjusto,
            )
            val racha = obtenerProgresoDeHoyUseCase.calcularRachaActual(sesionActual.espacioId)
            _uiState.update { it.copy(cerrado = true, rachaFinal = racha) }
        }
    }
}
