package com.aqpseller.lulaapp.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.model.SesionActual
import com.aqpseller.lulaapp.domain.usecase.nota.ActualizarOrdenNotaUseCase
import com.aqpseller.lulaapp.domain.usecase.nota.EliminarNotaUseCase
import com.aqpseller.lulaapp.domain.usecase.nota.ObtenerNotasUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LARGO_PREVIEW = 80

private fun Nota.aItemUi(): NotaListItemUi {
    val lineas = contenido.lines()
    val tituloDerivado = lineas.firstOrNull()?.trim().orEmpty().ifBlank { "(sin texto)" }
    val resto = if (titulo != null) contenido.trim() else lineas.drop(1).joinToString(" ").trim()
    return NotaListItemUi(
        id = id,
        titulo = titulo?.takeIf { it.isNotBlank() } ?: tituloDerivado,
        preview = if (resto.length > LARGO_PREVIEW) resto.take(LARGO_PREVIEW) + "…" else resto,
        fechaTexto = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaEdicion)),
        orden = orden,
    )
}

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerNotasUseCase: ObtenerNotasUseCase,
    private val eliminarNotaUseCase: EliminarNotaUseCase,
    private val actualizarOrdenNotaUseCase: ActualizarOrdenNotaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesListUiState())
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    private var sesion: SesionActual? = null

    init {
        viewModelScope.launch {
            val sesionActual = obtenerSesionActualUseCase()
            sesion = sesionActual
            // Notas es privado (vive detrás de Zona Privada) — siempre el espacio Personal, sin
            // importar cuál esté activo. Ver `SesionActual`, `08-decisiones-tecnicas.md`.
            obtenerNotasUseCase(sesionActual.espacioPersonalId)
                .map { notas -> notas.map { it.aItemUi() } }
                .collect { notas -> _uiState.value = NotesListUiState(cargando = false, notas = notas) }
        }
    }

    fun eliminar(notaId: String) {
        viewModelScope.launch {
            eliminarNotaUseCase(notaId, sesionActual().usuarioId)
        }
    }

    fun moverArriba(notaId: String) = mover(notaId, haciaArriba = true)
    fun moverAbajo(notaId: String) = mover(notaId, haciaArriba = false)

    /** Solo flechas ▲▼ para reordenar (no arrastrar) — a pedido del usuario, lo más fácil y
     * entendible posible. Intercambia el `orden` con el vecino inmediato en la lista ya
     * ordenada que se ve en pantalla. */
    private fun mover(notaId: String, haciaArriba: Boolean) {
        val notas = _uiState.value.notas
        val index = notas.indexOfFirst { it.id == notaId }
        if (index < 0) return
        val indiceVecino = if (haciaArriba) index - 1 else index + 1
        if (indiceVecino !in notas.indices) return
        val actual = notas[index]
        val vecino = notas[indiceVecino]
        viewModelScope.launch {
            val usuarioId = sesionActual().usuarioId
            actualizarOrdenNotaUseCase(actual.id, vecino.orden, usuarioId)
            actualizarOrdenNotaUseCase(vecino.id, actual.orden, usuarioId)
        }
    }

    private suspend fun sesionActual(): SesionActual = sesion ?: obtenerSesionActualUseCase().also { sesion = it }
}
