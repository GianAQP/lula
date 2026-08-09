package com.aqpseller.lulaapp.features.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.diario.EliminarEntradaDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.diario.ObtenerEntradasDiarioUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LARGO_EXTRACTO = 80

private fun EntradaDiario.aItemUi(): DiaryEntryItemUi {
    val extractoCrudo = texto.trim()
    return DiaryEntryItemUi(
        id = id,
        extracto = if (extractoCrudo.length > LARGO_EXTRACTO) extractoCrudo.take(LARGO_EXTRACTO) + "…" else extractoCrudo,
        fechaTexto = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fecha)),
    )
}

@HiltViewModel
class DiaryListViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerEntradasDiarioUseCase: ObtenerEntradasDiarioUseCase,
    private val eliminarEntradaDiarioUseCase: EliminarEntradaDiarioUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryListUiState())
    val uiState: StateFlow<DiaryListUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            // Diario es privado (vive detrás de Zona Privada) — siempre el espacio Personal, sin
            // importar cuál esté activo. Ver `SesionActual`, `08-decisiones-tecnicas.md`.
            obtenerEntradasDiarioUseCase(sesionActual.espacioPersonalId).collect { entradas ->
                _uiState.value = DiaryListUiState(cargando = false, entradas = entradas.map { it.aItemUi() })
            }
        }
    }

    fun eliminar(entradaId: String) {
        viewModelScope.launch {
            eliminarEntradaDiarioUseCase(entradaId, sesionActual().usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
