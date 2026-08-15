package com.aqpseller.lulaapp.features.daily_review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.actividadCuentaParaHoy
import com.aqpseller.lulaapp.core.utils.esHitoRacha
import com.aqpseller.lulaapp.core.utils.estadoDeHoy
import com.aqpseller.lulaapp.core.utils.mensajeAnticipacionHito
import com.aqpseller.lulaapp.core.utils.mensajeCierreDiario
import com.aqpseller.lulaapp.core.utils.mensajeHitoRacha
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerActividadesDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.calendario.ObtenerAgendaDelRangoUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.CerrarDiaUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerRegistroDiarioDeFechaUseCase
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
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerActividadesDeHoyUseCase: ObtenerActividadesDeHoyUseCase,
    private val cerrarDiaUseCase: CerrarDiaUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
    private val obtenerAgendaDelRangoUseCase: ObtenerAgendaDelRangoUseCase,
    private val obtenerRegistroDiarioDeFechaUseCase: ObtenerRegistroDiarioDeFechaUseCase,
    private val ajustesRepository: AjustesRepository,
) : ViewModel() {

    /** null (o ausente) = hoy. Un epoch day explícito viene de Calendario, para llenar o
     * actualizar el cierre de un día anterior. Ver `Plan/08-decisiones-tecnicas.md`. */
    private val fechaEpochDay: Long = savedStateHandle.get<String>("fecha")?.toLongOrNull()
        ?: DateTimeUtils.hoy().toEpochDays().toLong()
    private val fecha = DateTimeUtils.epochDaysToLocalDate(fechaEpochDay)
    private val esHoy = fecha == DateTimeUtils.hoy()

    private val _uiState = MutableStateFlow(CerrarDiaUiState(fecha = fecha, esHoy = esHoy))
    val uiState: StateFlow<CerrarDiaUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            if (esHoy) {
                // Una Cita de curso guarda su estado por sesión (`SesionCita`), no en
                // `Actividad.estado` — se necesita la sesión de hoy de cada curso para contarla
                // igual que Hoy la cuenta.
                val estadoSesionCitaHoy = obtenerAgendaDelRangoUseCase(sesionActual.espacioId, fecha, fecha)[fecha].orEmpty()
                    .filter { it.tipo == TipoActividad.CITA && it.sesionNumero != null }
                    .associate { it.actividadId to it.estado }
                val actividades = obtenerActividadesDeHoyUseCase(sesionActual.espacioId).first()
                    .filter { actividadCuentaParaHoy(it, estadoSesionCitaHoy) }
                // Antes esta pantalla siempre partía en blanco, incluso si hoy ya se había
                // cerrado — guardar sin tocar los campos borraba las respuestas que ya existían.
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
            } else {
                // Un día anterior no tiene forma confiable de "recalcular en vivo" cuántas
                // actividades se cumplieron ese día en particular — se escribe a mano (con lo ya
                // guardado como punto de partida, si ese día ya se había cerrado antes).
                val registroExistente = obtenerRegistroDiarioDeFechaUseCase(sesionActual.espacioId, fechaEpochDay)
                _uiState.update {
                    it.copy(
                        cargando = false,
                        actividadesCompletadas = registroExistente?.actividadesCompletadas ?: 0,
                        actividadesTotales = registroExistente?.actividadesTotales ?: 0,
                        yaExistiaRegistro = registroExistente != null,
                        queLogreInicial = registroExistente?.queLogre,
                        queCostoInicial = registroExistente?.queCosto,
                        queAjustoInicial = registroExistente?.queAjusto,
                    )
                }
            }
        }
    }

    /** Solo tiene efecto para un día que no es hoy — ver nota en `CerrarDiaUiState.esHoy`. */
    fun actualizarActividadesCompletadas(valor: Int) {
        _uiState.update { it.copy(actividadesCompletadas = valor) }
    }

    fun actualizarActividadesTotales(valor: Int) {
        _uiState.update { it.copy(actividadesTotales = valor) }
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
                fechaEpochDay = fechaEpochDay,
            )
            // Recalcular siempre, no solo si es hoy: cerrar un día anterior puede rellenar un
            // hueco que hasta ahora cortaba la racha, extendiéndola hacia atrás.
            val racha = obtenerProgresoDeHoyUseCase.calcularRachaActual(sesionActual.espacioId)

            // Si la racha se rompió (volvió a 0 o 1), se olvida el último hito celebrado — una
            // racha nueva puede volver a celebrar 7/21/30 desde cero. Ver `08-decisiones-tecnicas.md`.
            if (racha <= 1) ajustesRepository.setUltimoHitoRachaCelebrado(0)
            val ultimoHitoCelebrado = ajustesRepository.obtenerUltimoHitoRachaCelebrado()
            val hitoAlcanzado = if (esHitoRacha(racha) && racha > ultimoHitoCelebrado) racha else null
            val mensaje = if (hitoAlcanzado != null) {
                ajustesRepository.setUltimoHitoRachaCelebrado(hitoAlcanzado)
                mensajeHitoRacha(hitoAlcanzado)
            } else {
                mensajeAnticipacionHito(racha) ?: mensajeCierreDiario()
            }
            _uiState.update { it.copy(cerrado = true, rachaFinal = racha, hitoAlcanzado = hitoAlcanzado, mensajeCierre = mensaje) }
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
