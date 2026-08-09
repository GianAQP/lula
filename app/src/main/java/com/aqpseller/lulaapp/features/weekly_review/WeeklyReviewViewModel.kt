package com.aqpseller.lulaapp.features.weekly_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHabitosUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHistorialHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerHistorialDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.registrosemanal.GuardarRevisionSemanalUseCase
import com.aqpseller.lulaapp.domain.usecase.registrosemanal.ObtenerRevisionSemanalGuardadaUseCase
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
class WeeklyReviewViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerHistorialDiarioUseCase: ObtenerHistorialDiarioUseCase,
    private val obtenerHabitosUseCase: ObtenerHabitosUseCase,
    private val obtenerHistorialHabitoUseCase: ObtenerHistorialHabitoUseCase,
    private val obtenerRevisionSemanalGuardadaUseCase: ObtenerRevisionSemanalGuardadaUseCase,
    private val guardarRevisionSemanalUseCase: GuardarRevisionSemanalUseCase,
    private val ajustesRepository: AjustesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReviewUiState())
    val uiState: StateFlow<WeeklyReviewUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual

            val diaConfigurado = ajustesRepository.observarDiaRevisionSemanal().first()
            val activada = DateTimeUtils.numeroDiaIsoDeHoy() >= diaConfigurado
            if (!activada) {
                _uiState.update {
                    it.copy(cargando = false, activada = false, nombreDiaActivacion = DateTimeUtils.nombreDiaIso(diaConfigurado))
                }
                return@launch
            }

            val inicioSemana = DateTimeUtils.inicioDeSemanaEpochDias()
            val finSemana = DateTimeUtils.finDeSemanaEpochDias()
            val registrosSemana = obtenerHistorialDiarioUseCase(sesionActual.espacioId).first()
                .filter { it.fecha in inicioSemana..finSemana }
                .associateBy { it.fecha }

            val totalCompletadas = registrosSemana.values.sumOf { it.actividadesCompletadas }
            val totalActividades = registrosSemana.values.sumOf { it.actividadesTotales }
            val cumplimiento = if (totalActividades > 0) totalCompletadas * 100 / totalActividades else 0

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

            val habitos = obtenerHabitosUseCase(sesionActual.espacioId).first()
            val destacados = habitos.map { habito ->
                val confirmados = obtenerHistorialHabitoUseCase.ultimosDias(habito.id, 7)
                    .count { it.estado == EstadoActividad.CONFIRMADO }
                HabitoDestacadoUi(nombre = habito.nombre, porcentaje = confirmados * 100 / 7)
            }
            val mejor = destacados.maxByOrNull { it.porcentaje }
            val peor = destacados.filter { it.nombre != mejor?.nombre }.minByOrNull { it.porcentaje }

            val revisionGuardada = obtenerRevisionSemanalGuardadaUseCase(sesionActual.espacioId)

            _uiState.update {
                it.copy(
                    cargando = false,
                    cumplimientoPorcentaje = cumplimiento,
                    rachaMaxima = rachaMaxima,
                    mejorHabito = mejor,
                    peorHabito = peor,
                    guardada = revisionGuardada != null,
                    queLogreGuardado = revisionGuardada?.queLogre.orEmpty(),
                    queNoFuncionoGuardado = revisionGuardada?.queNoFunciono.orEmpty(),
                    queAjustoGuardado = revisionGuardada?.queAjusto.orEmpty(),
                )
            }
        }
    }

    fun guardarRevision(queLogre: String?, queNoFunciono: String?, queAjusto: String?) {
        viewModelScope.launch {
            val sesionActual = sesionActual()
            val estado = _uiState.value
            guardarRevisionSemanalUseCase(
                espacioId = sesionActual.espacioId,
                usuarioId = sesionActual.usuarioId,
                cumplimientoGeneralPorcentaje = estado.cumplimientoPorcentaje,
                rachaMaxima = estado.rachaMaxima,
                queLogre = queLogre,
                queNoFunciono = queNoFunciono,
                queAjusto = queAjusto,
            )
            _uiState.update { it.copy(guardada = true, guardadoExitoso = true) }
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
