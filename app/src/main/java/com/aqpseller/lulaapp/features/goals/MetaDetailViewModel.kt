package com.aqpseller.lulaapp.features.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHistorialHabitoUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.CompartirActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.AgregarProgresoMetaUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.EliminarMetaUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.ObtenerDetalleMetaUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Única categoría de Finanzas que cuenta para una meta "por monto" — ver `08-decisiones-tecnicas.md`. */
private const val CATEGORIA_AHORRO = "Ahorro"

@HiltViewModel
class MetaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerDetalleMetaUseCase: ObtenerDetalleMetaUseCase,
    private val obtenerHistorialHabitoUseCase: ObtenerHistorialHabitoUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
    private val agregarProgresoMetaUseCase: AgregarProgresoMetaUseCase,
    private val eliminarMetaUseCase: EliminarMetaUseCase,
    private val compartirActividadUseCase: CompartirActividadUseCase,
) : ViewModel() {

    val metaId: String = checkNotNull(savedStateHandle["metaId"])

    private val _uiState = MutableStateFlow(MetaDetailUiState())
    val uiState: StateFlow<MetaDetailUiState> = _uiState.asStateFlow()

    private val _solicitudEnviada = MutableStateFlow(false)
    val solicitudEnviada: StateFlow<Boolean> = _solicitudEnviada.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        cargar()
    }

    /**
     * Navigation Compose reutiliza esta misma instancia del ViewModel al volver de Editar
     * (la pantalla no se recrea, solo se recompone) — sin volver a llamar `cargar()`, el
     * `uiState` se quedaba con la foto de antes de editar hasta salir y volver a entrar.
     */
    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            val meta = obtenerDetalleMetaUseCase(metaId) ?: return@launch
            val habitoId = meta.actividadesVinculadasIds.firstOrNull()
            val esPorHabito = meta.comoSeMide == ComoSeMideMeta.POR_HABITO && habitoId != null
            val esPorMonto = meta.comoSeMide == ComoSeMideMeta.POR_MONTO
            val progreso = when {
                esPorHabito -> {
                    val dias = meta.valorObjetivo.toInt().coerceAtLeast(1)
                    obtenerHistorialHabitoUseCase.ultimosDias(habitoId!!, dias).count { it.estado == EstadoActividad.CONFIRMADO }.toDouble()
                }
                esPorMonto -> obtenerBalanceMesUseCase(sesionActual.espacioId, 0L, DateTimeUtils.ahoraEpochMillis())
                    .first()
                    .filter { it.tipo == TipoMovimientoFinanciero.EGRESO && it.categoria.equals(CATEGORIA_AHORRO, ignoreCase = true) }
                    .sumOf { it.monto }
                else -> meta.valorActual
            }
            val nombreHabito = if (esPorHabito) obtenerDetalleActividadUseCase(habitoId!!)?.nombre else null
            _uiState.update {
                it.copy(
                    cargando = false,
                    nombre = meta.nombre,
                    comoSeMide = meta.comoSeMide,
                    progreso = progreso,
                    objetivo = meta.valorObjetivo,
                    nombreHabitoVinculado = nombreHabito,
                )
            }
        }
    }

    fun agregarProgreso(incremento: Double) {
        if (incremento <= 0) return
        viewModelScope.launch {
            agregarProgresoMetaUseCase(metaId, incremento, sesionActual().usuarioId)
            cargar()
        }
    }

    fun eliminar() {
        viewModelScope.launch {
            eliminarMetaUseCase(metaId, sesionActual().usuarioId)
            _uiState.update { it.copy(eliminada = true) }
        }
    }

    fun compartir(contacto: String, permiso: PermisoCompartir) {
        viewModelScope.launch {
            compartirActividadUseCase(sesionActual().usuarioId, metaId, _uiState.value.nombre, contacto, permiso)
            _solicitudEnviada.value = true
        }
    }

    fun solicitudEnviadaMostrada() {
        _solicitudEnviada.value = false
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
