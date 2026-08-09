package com.aqpseller.lulaapp.features.important_dates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.actividad.EliminarActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.fechaimportante.ObtenerFechasImportantesUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun etiquetaRecurrencia(recurrencia: Recurrencia): String = when (recurrencia) {
    Recurrencia.UNICA -> "Una vez"
    Recurrencia.SEMANAL -> "Cada semana"
    Recurrencia.ANUAL -> "Cada año"
}

@HiltViewModel
class ImportantDatesListViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerFechasImportantesUseCase: ObtenerFechasImportantesUseCase,
    private val eliminarActividadUseCase: EliminarActividadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportantDatesListUiState())
    val uiState: StateFlow<ImportantDatesListUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            obtenerFechasImportantesUseCase(sesionActual.espacioId)
                .map { lista ->
                    lista.mapNotNull { actividad ->
                        val detalle = actividad.detalle as? ActividadDetalle.FechaImportante ?: return@mapNotNull null
                        FechaImportanteListItemUi(
                            id = actividad.id,
                            nombre = actividad.nombre,
                            fechaTexto = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(detalle.fechaBase)),
                            recurrenciaTexto = etiquetaRecurrencia(detalle.recurrencia),
                        )
                    }
                }
                .collect { fechas -> _uiState.value = ImportantDatesListUiState(cargando = false, fechas = fechas) }
        }
    }

    fun eliminar(actividadId: String) {
        viewModelScope.launch {
            eliminarActividadUseCase(actividadId, sesionActual().usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
