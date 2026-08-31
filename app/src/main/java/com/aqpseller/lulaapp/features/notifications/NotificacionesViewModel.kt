package com.aqpseller.lulaapp.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.Notificacion
import com.aqpseller.lulaapp.domain.repository.NotificacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificacionItemUi(
    val id: String,
    val emoji: String,
    val titulo: String,
    val cuerpo: String,
    val fecha: Long,
    val leido: Boolean,
    val solicitudId: String?,
)

data class NotificacionesUiState(
    val cargando: Boolean = true,
    val notificaciones: List<NotificacionItemUi> = emptyList(),
)

/**
 * Historial permanente de avisos — a diferencia de la notificación del sistema (que se posta y
 * se pierde), esta lista queda guardada en la app, con leído/no leído, igual que el historial de
 * notificaciones de cualquier app real (a pedido explícito del usuario, tras confundir la
 * primera versión con solo un acceso directo a "Mi círculo de cuidado"). Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val notificacionRepository: NotificacionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacionesUiState())
    val uiState: StateFlow<NotificacionesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificacionRepository.observarTodas().collect { notificaciones ->
                _uiState.update {
                    it.copy(cargando = false, notificaciones = notificaciones.map { n -> n.aUi() })
                }
            }
        }
    }

    /** Marca leída al tocarla — si venía de una invitación, la pantalla navega a "Mi círculo de
     * cuidado" aparte (ahí se ve si sigue pendiente o ya se respondió). */
    fun marcarLeida(id: String) {
        viewModelScope.launch { notificacionRepository.marcarLeida(id) }
    }
}

private fun Notificacion.aUi() = NotificacionItemUi(
    id = id,
    emoji = emoji,
    titulo = titulo,
    cuerpo = cuerpo,
    fecha = fecha,
    leido = leido,
    solicitudId = solicitudId,
)
