package com.aqpseller.lulaapp.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.actividadCuentaParaHoy
import com.aqpseller.lulaapp.core.utils.esTareaPendienteDeHoyOVencida
import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.usecase.actividad.CerrarTareasVinculadasVencidasUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.MarcarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerActividadesDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHistorialHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerRutinasUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ResponderRevisionHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.calendario.ObtenerAgendaDelRangoUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.ExtenderSesionesCursoSiHaceFaltaUseCase
import com.aqpseller.lulaapp.domain.usecase.cita.ObtenerCitasUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.ObtenerEspaciosDeUsuarioUseCase
import com.aqpseller.lulaapp.domain.usecase.fechaimportante.ObtenerFechasImportantesUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.ObtenerListasUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.MarcarTomaMedicamentoUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.ObtenerMedicamentosDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.medicamento.ObtenerMedicamentosUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.MarcarHitoMetaCelebradoUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.MetaConProgreso
import com.aqpseller.lulaapp.domain.usecase.meta.ObtenerMetasConProgresoUseCase
import com.aqpseller.lulaapp.domain.usecase.nota.ObtenerNotasUseCase
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerActividadesDeHoyUseCase: ObtenerActividadesDeHoyUseCase,
    private val marcarActividadUseCase: MarcarActividadUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
    private val obtenerMetasConProgresoUseCase: ObtenerMetasConProgresoUseCase,
    private val marcarHitoMetaCelebradoUseCase: MarcarHitoMetaCelebradoUseCase,
    private val obtenerRutinasUseCase: ObtenerRutinasUseCase,
    private val obtenerListasUseCase: ObtenerListasUseCase,
    private val ajustesRepository: AjustesRepository,
    private val obtenerHistorialHabitoUseCase: ObtenerHistorialHabitoUseCase,
    private val responderRevisionHabitoUseCase: ResponderRevisionHabitoUseCase,
    private val obtenerMedicamentosDeHoyUseCase: ObtenerMedicamentosDeHoyUseCase,
    private val marcarTomaMedicamentoUseCase: MarcarTomaMedicamentoUseCase,
    private val obtenerMedicamentosUseCase: ObtenerMedicamentosUseCase,
    private val obtenerCitasUseCase: ObtenerCitasUseCase,
    private val obtenerFechasImportantesUseCase: ObtenerFechasImportantesUseCase,
    private val obtenerNotasUseCase: ObtenerNotasUseCase,
    private val cerrarTareasVinculadasVencidasUseCase: CerrarTareasVinculadasVencidasUseCase,
    private val espacioRepository: EspacioRepository,
    private val obtenerEspaciosDeUsuarioUseCase: ObtenerEspaciosDeUsuarioUseCase,
    private val obtenerAgendaDelRangoUseCase: ObtenerAgendaDelRangoUseCase,
    private val extenderSesionesCursoSiHaceFaltaUseCase: ExtenderSesionesCursoSiHaceFaltaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            refrescarProgresoDelDia(sesionActual)

            launch {
                cerrarTareasVinculadasVencidasUseCase(sesionActual.espacioId, sesionActual.usuarioId)
            }

            launch {
                // Un curso de Cita sin cantidad fija (ej. masajes) solo genera sesiones hasta un
                // horizonte de 90 días — esto lo completa cuando queda poco margen, para que
                // nunca se le acabe el programa sin que el usuario tenga que volver a editarlo.
                obtenerCitasUseCase(sesionActual.espacioId).first().forEach { cita ->
                    extenderSesionesCursoSiHaceFaltaUseCase(cita)
                }
            }

            launch {
                // Aviso de "pendientes en Familia" — solo tiene sentido cuando ese espacio existe
                // y no es ya el espacio activo (si ya estás ahí, no hace falta avisarte).
                val familia = obtenerEspaciosDeUsuarioUseCase(sesionActual.usuarioId).first()
                    .firstOrNull { it.tipo == TipoEspacio.FAMILIA }
                if (familia != null && familia.id != sesionActual.espacioId) {
                    obtenerActividadesDeHoyUseCase(familia.id).collect { actividades ->
                        val pendientes = actividades.count {
                            (it.tipo == TipoActividad.HABITO || (it.tipo == TipoActividad.TAREA && esTareaPendienteDeHoyOVencida(it.detalle))) &&
                                it.estado != EstadoActividad.CONFIRMADO
                        }
                        _uiState.update { it.copy(pendientesEnFamilia = pendientes) }
                    }
                }
            }

            launch {
                // Citas y Fechas importantes de hoy — reusa la misma consulta que arma el
                // Calendario (rango de 1 día), en vez de duplicar la lógica de recurrencia de
                // Fecha importante acá. Antes no aparecían para nada en Hoy aunque estuvieran
                // programadas justo para hoy (ver feedback del usuario, 2026-07-30).
                val hoy = DateTimeUtils.hoy()
                val agendaDeHoy = obtenerAgendaDelRangoUseCase(sesionActual.espacioId, hoy, hoy)[hoy].orEmpty()
                _uiState.update {
                    it.copy(
                        citasDeHoy = agendaDeHoy.filter { item -> item.tipo == TipoActividad.CITA },
                        fechasImportantesDeHoy = agendaDeHoy.filter { item -> item.tipo == TipoActividad.FECHA_IMPORTANTE },
                    )
                }
            }

            launch {
                obtenerActividadesDeHoyUseCase(sesionActual.espacioId).collect { actividades ->
                    val revisionesPendientes = actividades
                        .filter { it.tipo == TipoActividad.HABITO }
                        .mapNotNull { actividadRevisionPendienteOuNull(it) }
                    _uiState.update { estado ->
                        estado.copy(
                            cargando = false,
                            actividadesManana = actividades.aUi(TipoActividad.HABITO, MomentoDelDia.MANANA),
                            actividadesTarde = actividades.aUi(TipoActividad.HABITO, MomentoDelDia.TARDE),
                            actividadesNoche = actividades.aUi(TipoActividad.HABITO, MomentoDelDia.NOCHE),
                            tareasDeHoy = actividades
                                .filter { it.tipo == TipoActividad.TAREA && actividadCuentaParaHoy(it) }
                                .map {
                                    ActividadUi(
                                        id = it.id,
                                        nombre = it.nombre,
                                        estado = it.estado,
                                        tipo = it.tipo,
                                        horaRecordatorio = (it.detalle as? ActividadDetalle.Tarea)?.horaRecordatorio,
                                    )
                                },
                            revisionesHabitoPendientes = revisionesPendientes,
                        )
                    }
                }
            }

            launch {
                obtenerBalanceMesUseCase(
                    sesionActual.espacioId,
                    DateTimeUtils.inicioDeHoyEpochMillis(),
                    DateTimeUtils.finDeHoyEpochMillis(),
                ).collect { movimientos ->
                    val totalGastos = movimientos
                        .filter { it.tipo == TipoMovimientoFinanciero.EGRESO }
                        .sumOf { it.monto }
                    _uiState.update { it.copy(gastosHoyTotal = totalGastos) }
                }
            }

            launch {
                obtenerMetasConProgresoUseCase(sesionActual.espacioId).collect { metas ->
                    _uiState.update { it.copy(hayMetas = metas.isNotEmpty(), metasActivas = metas) }
                }
            }

            launch {
                obtenerRutinasUseCase(sesionActual.espacioId).collect { rutinas ->
                    _uiState.update { it.copy(hayRutinas = rutinas.isNotEmpty()) }
                }
            }

            launch {
                obtenerListasUseCase(sesionActual.espacioId).collect { listas ->
                    _uiState.update { it.copy(hayListas = listas.isNotEmpty()) }
                }
            }

            launch {
                ajustesRepository.observarSonidoCheckHabilitado().collect { habilitado ->
                    _uiState.update { it.copy(sonidoCheckHabilitado = habilitado) }
                }
            }

            launch {
                obtenerMedicamentosDeHoyUseCase(sesionActual.espacioId).collect { medicamentos ->
                    _uiState.update { it.copy(medicamentosDeHoy = medicamentos) }
                }
            }

            launch {
                obtenerFechasImportantesUseCase(sesionActual.espacioId).collect { fechas ->
                    _uiState.update { it.copy(hayFechasImportantes = fechas.isNotEmpty()) }
                }
            }

            launch {
                obtenerNotasUseCase(sesionActual.espacioId).collect { notas ->
                    _uiState.update { it.copy(hayNotas = notas.isNotEmpty()) }
                }
            }

            launch {
                combine(
                    obtenerMedicamentosUseCase(sesionActual.espacioId),
                    obtenerCitasUseCase(sesionActual.espacioId),
                ) { medicamentos, citas -> medicamentos.isNotEmpty() || citas.isNotEmpty() }
                    .collect { hayAlgo -> _uiState.update { it.copy(hayMedicamentosOCitas = hayAlgo) } }
            }
        }
    }

    fun marcarActividad(actividadId: String, estado: EstadoActividad) {
        viewModelScope.launch {
            marcarActividadUseCase(actividadId, estado, sesionActual().usuarioId)
        }
    }

    fun marcarToma(actividadId: String, horario: String, estado: EstadoActividad) {
        viewModelScope.launch {
            marcarTomaMedicamentoUseCase(actividadId, horario, estado, sesionActual().usuarioId)
        }
    }

    /** Actualiza en memoria además de guardar, para que la tarjeta desaparezca al toque sin
     * esperar a que el `Flow` de metas vuelva a emitir. */
    fun marcarHitoMetaCelebrado(metaConProgreso: MetaConProgreso) {
        viewModelScope.launch {
            marcarHitoMetaCelebradoUseCase(metaConProgreso.meta.id, metaConProgreso.hitoActual, sesionActual().usuarioId)
        }
        _uiState.update { estado ->
            estado.copy(
                metasActivas = estado.metasActivas.map {
                    if (it.meta.id == metaConProgreso.meta.id) {
                        it.copy(meta = it.meta.copy(ultimoHitoCelebrado = metaConProgreso.hitoActual))
                    } else {
                        it
                    }
                },
            )
        }
    }

    fun responderRevisionSubir(revision: RevisionHabitoUi) {
        viewModelScope.launch {
            responderRevisionHabitoUseCase.subir(
                revision.actividadId,
                revision.duracionActualMin,
                revision.incrementoMin,
                revision.duracionObjetivoMin,
                revision.frecuenciaRevisionDias,
                sesionActual().usuarioId,
            )
            quitarRevisionPendiente(revision.actividadId)
        }
    }

    fun responderRevisionMantener(revision: RevisionHabitoUi) {
        viewModelScope.launch {
            responderRevisionHabitoUseCase.mantener(
                revision.actividadId,
                revision.duracionActualMin,
                revision.frecuenciaRevisionDias,
                sesionActual().usuarioId,
            )
            quitarRevisionPendiente(revision.actividadId)
        }
    }

    fun responderRevisionRecordarDespues(revision: RevisionHabitoUi) {
        viewModelScope.launch {
            responderRevisionHabitoUseCase.recordarDespues(revision.actividadId, revision.duracionActualMin, sesionActual().usuarioId)
            quitarRevisionPendiente(revision.actividadId)
        }
    }

    /**
     * Quita la tarjeta localmente en vez de esperar a que el `Flow` de Hoy se vuelva a emitir
     * — escribir en `habito_detalle` no invalida esa consulta (solo referencia `actividad` y
     * `registro_actividad`, ver `Plan/08-decisiones-tecnicas.md`), y la tarjeta es efímera, no
     * necesita la misma reactividad completa que el resto de Hoy.
     */
    private fun quitarRevisionPendiente(actividadId: String) {
        _uiState.update {
            it.copy(revisionesHabitoPendientes = it.revisionesHabitoPendientes.filterNot { r -> r.actividadId == actividadId })
        }
    }

    private suspend fun actividadRevisionPendienteOuNull(actividad: Actividad): RevisionHabitoUi? {
        val detalle = actividad.detalle as? ActividadDetalle.Habito ?: return null
        if (!detalle.esProgresivo) return null
        val proxima = detalle.proximaRevisionEpochDay ?: return null
        val hoy = DateTimeUtils.hoy().toEpochDays().toLong()
        if (hoy < proxima) return null
        val duracionActual = detalle.duracionActualMin ?: return null
        val objetivo = detalle.duracionObjetivoMin ?: return null
        if (duracionActual >= objetivo) return null
        val frecuencia = detalle.frecuenciaRevisionDias ?: return null
        val incremento = detalle.incrementoMin ?: return null
        val cumplidos = obtenerHistorialHabitoUseCase.ultimosDias(actividad.id, frecuencia)
            .count { it.estado == EstadoActividad.CONFIRMADO }
        return RevisionHabitoUi(
            actividadId = actividad.id,
            nombre = actividad.nombre,
            duracionActualMin = duracionActual,
            incrementoMin = incremento,
            duracionObjetivoMin = objetivo,
            frecuenciaRevisionDias = frecuencia,
            diasCumplidos = cumplidos,
            diasTotales = frecuencia,
        )
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }

    private suspend fun refrescarProgresoDelDia(sesionActual: SesionActual) {
        val racha = obtenerProgresoDeHoyUseCase.calcularRachaActual(sesionActual.espacioId)
        val registroHoy = obtenerProgresoDeHoyUseCase.registroDeHoy(sesionActual.espacioId)
        _uiState.update { it.copy(racha = racha, diaYaCerrado = registroHoy != null) }
    }

    private fun List<Actividad>.aUi(
        tipo: TipoActividad,
        momento: MomentoDelDia,
    ) = filter { it.tipo == tipo && it.momentoDelDia == momento }
        .map {
            ActividadUi(
                id = it.id,
                nombre = it.nombre,
                estado = it.estado,
                tipo = it.tipo,
                horaRecordatorio = (it.detalle as? ActividadDetalle.Habito)?.horaRecordatorio,
            )
        }
}
