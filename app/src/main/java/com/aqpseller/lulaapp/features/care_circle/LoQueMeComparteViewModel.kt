package com.aqpseller.lulaapp.features.care_circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadCompartidaRemota
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.carecircle.DejarDeVerActividadCompartidaUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.ObtenerActividadesCompartidasConmigoUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.SolicitarRecordatorioUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActividadCompartidaUi(
    val solicitudId: String,
    val actividadId: String,
    val deFirebaseUid: String,
    val emoji: String,
    val nombre: String,
    val deNombre: String,
    val permiso: PermisoCompartir,
    val subtitulo: String,
)

data class LoQueMeComparteUiState(
    val cargando: Boolean = true,
    val actividades: List<ActividadCompartidaUi> = emptyList(),
    /** true un momento después de tocar "Recordarle" — la pantalla lo muestra como confirmación
     * breve y se limpia sola. */
    val recordatorioEnviadoA: String? = null,
)

private fun emojiPara(tipo: TipoActividad): String = when (tipo) {
    TipoActividad.HABITO -> "🌱"
    TipoActividad.TAREA -> "📝"
    TipoActividad.RUTINA -> "🔁"
    TipoActividad.MEDICAMENTO -> "💊"
    TipoActividad.CITA -> "🩺"
    TipoActividad.FECHA_IMPORTANTE -> "🎉"
}

private fun subtituloPara(item: ActividadCompartidaRemota): String = when (item.actividad.tipo) {
    TipoActividad.HABITO -> when (item.actividad.estado) {
        EstadoActividad.CONFIRMADO -> "✅ Ya lo hizo hoy"
        EstadoActividad.OMITIDO -> "⏭️ Lo omitió hoy"
        EstadoActividad.SIN_CONFIRMAR -> "⏳ Todavía no hoy"
    }
    TipoActividad.TAREA -> when (item.actividad.estado) {
        EstadoActividad.CONFIRMADO -> "✅ Completada"
        EstadoActividad.OMITIDO -> "⏭️ Omitida"
        EstadoActividad.SIN_CONFIRMAR -> "⏳ Pendiente"
    }
    TipoActividad.MEDICAMENTO -> {
        val hechas = item.tomasRecientes.count { it.estado == EstadoActividad.CONFIRMADO }
        val total = item.tomasRecientes.size
        if (total == 0) "Sin tomas registradas todavía" else "💊 $hechas de $total tomas recientes"
    }
    TipoActividad.CITA -> {
        val detalle = item.detalle as? ActividadDetalle.Cita
        if (detalle?.esCurso == true) {
            val hechas = item.sesionesCita.count { it.estado == EstadoActividad.CONFIRMADO }
            "🩺 ${hechas} de ${item.sesionesCita.size} sesiones cumplidas"
        } else {
            "🩺 Cita programada"
        }
    }
    TipoActividad.RUTINA -> "🔁 Rutina compartida"
    TipoActividad.FECHA_IMPORTANTE -> "🎉 Fecha importante"
}

@HiltViewModel
class LoQueMeComparteViewModel @Inject constructor(
    private val obtenerActividadesCompartidasConmigoUseCase: ObtenerActividadesCompartidasConmigoUseCase,
    private val dejarDeVerActividadCompartidaUseCase: DejarDeVerActividadCompartidaUseCase,
    private val solicitarRecordatorioUseCase: SolicitarRecordatorioUseCase,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val usuarioRepository: UsuarioRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoQueMeComparteUiState())
    val uiState: StateFlow<LoQueMeComparteUiState> = _uiState.asStateFlow()

    fun dejarDeVer(solicitudId: String) {
        viewModelScope.launch {
            val usuarioId = obtenerSesionActualUseCase().usuarioId
            dejarDeVerActividadCompartidaUseCase(solicitudId, usuarioId)
        }
    }

    /** Solo tiene sentido si `item.permiso == PUEDE_VER_Y_RECORDAR` — la pantalla ya solo
     * muestra el botón en ese caso. */
    fun recordar(item: ActividadCompartidaUi) {
        viewModelScope.launch {
            solicitarRecordatorioUseCase(item.actividadId, item.nombre, item.deFirebaseUid)
            _uiState.update { it.copy(recordatorioEnviadoA = item.deNombre) }
        }
    }

    fun recordatorioEnviadoMostrado() {
        _uiState.update { it.copy(recordatorioEnviadoA = null) }
    }

    init {
        viewModelScope.launch {
            val correo = usuarioRepository.observarUsuario().first()?.correo ?: ""
            obtenerActividadesCompartidasConmigoUseCase(correo).collect { actividades ->
                _uiState.update {
                    it.copy(
                        cargando = false,
                        actividades = actividades.map { item ->
                            ActividadCompartidaUi(
                                solicitudId = item.solicitudId,
                                actividadId = item.actividad.id,
                                deFirebaseUid = item.deFirebaseUid,
                                emoji = emojiPara(item.actividad.tipo),
                                nombre = item.actividad.nombre,
                                deNombre = item.deNombre,
                                permiso = item.permiso,
                                subtitulo = subtituloPara(item),
                            )
                        },
                    )
                }
            }
        }
    }
}
