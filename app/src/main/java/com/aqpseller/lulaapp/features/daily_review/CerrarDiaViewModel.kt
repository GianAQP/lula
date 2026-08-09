package com.aqpseller.lulaapp.features.daily_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.actividadCuentaParaHoy
import com.aqpseller.lulaapp.core.utils.estadoDeHoy
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerActividadesDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.calendario.ObtenerAgendaDelRangoUseCase
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
    private val obtenerAgendaDelRangoUseCase: ObtenerAgendaDelRangoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CerrarDiaUiState())
    val uiState: StateFlow<CerrarDiaUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            // Una Cita de curso guarda su estado por sesión (`SesionCita`), no en `Actividad.estado`
            // — se necesita la sesión de hoy de cada curso para contarla igual que Hoy la cuenta.
            val hoy = DateTimeUtils.hoy()
            val estadoSesionCitaHoy = obtenerAgendaDelRangoUseCase(sesionActual.espacioId, hoy, hoy)[hoy].orEmpty()
                .filter { it.tipo == TipoActividad.CITA && it.sesionNumero != null }
                .associate { it.actividadId to it.estado }
            val actividades = obtenerActividadesDeHoyUseCase(sesionActual.espacioId).first()
                .filter { actividadCuentaParaHoy(it, estadoSesionCitaHoy) }
            // Antes esta pantalla siempre partía en blanco, incluso si hoy ya se había cerrado
            // — guardar sin tocar los campos borraba las respuestas que ya existían. Ver
            // `Plan/08-decisiones-tecnicas.md`.
            val registroExistente = obtenerProgresoDeHoyUseCase.registroDeHoy(sesionActual.espacioId)
            _uiState.update {
                it.copy(
                    cargando = false,
                    actividadesCompletadas = actividades.count { a -> estadoDeHoy(a, estadoSesionCitaHoy) == EstadoActividad.CONFIRMADO },
                    actividadesTotales = actividades.size,
                    yaExistiaRegistro = registroExistente != null,
                    queLogreInicial = registroExistente?.queLogre,
                    queCostoInicial = registroExistente?.queCosto,
                    queAjustoInicial = registroExistente?.queAjusto,
                )
            }
        }
    }

    fun cerrarDia(queLogre: String?, queCosto: String?, queAjusto: String?) {
        viewModelScope.launch {
            val sesionActual = sesionActual()
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

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
