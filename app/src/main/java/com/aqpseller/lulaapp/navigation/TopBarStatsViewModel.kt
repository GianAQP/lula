package com.aqpseller.lulaapp.navigation

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.MainActivity
import com.aqpseller.lulaapp.core.notifications.NotificationChannels
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.decodificarCodigoCompartirQr
import com.aqpseller.lulaapp.core.utils.decodificarCodigoEspacioQr
import com.aqpseller.lulaapp.core.utils.decodificarContactoQr
import com.aqpseller.lulaapp.core.utils.decodificarListaQr
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.TipoSolicitud
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.NotificacionRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.carecircle.EventoSolicitud
import com.aqpseller.lulaapp.domain.usecase.carecircle.ReclamarCodigoCompartirActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.carecircle.ResultadoReclamoCompartir
import com.aqpseller.lulaapp.domain.usecase.carecircle.SincronizarYDetectarEventosSolicitudesUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.ResultadoUnionEspacio
import com.aqpseller.lulaapp.domain.usecase.espacio.SincronizarEspacioFamiliaUseCase
import com.aqpseller.lulaapp.domain.usecase.espacio.UnirseAEspacioConCodigoUseCase
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.lista.ImportarListaDesdeQrUseCase
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopBarStatsUiState(
    val racha: Int = 0,
    val gastosHoyTotal: Double = 0.0,
    /** Cuenta del historial permanente de notificaciones (`NotificacionRepository`), no de
     * solicitudes pendientes — ver `Plan/08-decisiones-tecnicas.md`. */
    val notificacionesNoLeidas: Int = 0,
    /** Null = espacio Personal — ver "banda de espacio activo", `Plan/08-decisiones-tecnicas.md`. */
    val nombreEspacioActivo: String? = null,
    val mensaje: String? = null,
    /** Cuando escaneo el código de contacto de alguien — la UI lo copia al portapapeles. */
    val correoParaCopiar: String? = null,
)

/**
 * Racha, gastos de hoy e invitaciones pendientes — viven en `LulaTopBar` (mismo nivel que el
 * menú "⋮") para que se vean en cualquier pantalla, no solo en Hoy, a pedido del usuario.
 */
@HiltViewModel
class TopBarStatsViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
    private val espacioRepository: EspacioRepository,
    private val usuarioRepository: UsuarioRepository,
    private val importarListaDesdeQrUseCase: ImportarListaDesdeQrUseCase,
    private val sincronizarEspacioFamiliaUseCase: SincronizarEspacioFamiliaUseCase,
    private val unirseAEspacioConCodigoUseCase: UnirseAEspacioConCodigoUseCase,
    private val reclamarCodigoCompartirActividadUseCase: ReclamarCodigoCompartirActividadUseCase,
    private val compartirSyncRepository: CompartirSyncRepository,
    private val sincronizarYDetectarEventosSolicitudesUseCase: SincronizarYDetectarEventosSolicitudesUseCase,
    private val notificacionRepository: NotificacionRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopBarStatsUiState())
    val uiState: StateFlow<TopBarStatsUiState> = _uiState.asStateFlow()

    private var espacioIdSincronizado: String? = null
    private var jobSincronizacionEspacio: Job? = null

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            refrescarRacha(sesion.espacioId)
            refrescarEspacioActivo(sesion.espacioId, sesion.usuarioId)

            launch {
                obtenerBalanceMesUseCase(sesion.espacioId, DateTimeUtils.inicioDeHoyEpochMillis(), DateTimeUtils.finDeHoyEpochMillis())
                    .collect { movimientos ->
                        val gastos = movimientos.filter { it.tipo == TipoMovimientoFinanciero.EGRESO }.sumOf { it.monto }
                        _uiState.update { it.copy(gastosHoyTotal = gastos) }
                    }
            }

            launch {
                notificacionRepository.observarNoLeidas().collect { cantidad ->
                    _uiState.update { it.copy(notificacionesNoLeidas = cantidad) }
                }
            }

            launch {
                // "Recordarle" (Círculo de cuidado, permiso PUEDE_VER_Y_RECORDAR) — best-effort:
                // solo se muestra si esta pantalla sigue viva cuando llega. Vacío hasta que la
                // cuenta esté vinculada con Google. Ver `Plan/08-decisiones-tecnicas.md`.
                val firebaseUid = usuarioRepository.observarUsuario().first()?.firebaseUid ?: return@launch
                compartirSyncRepository.escucharRecordatoriosSolicitados(firebaseUid).collect { recordatorios ->
                    recordatorios.forEach { recordatorio ->
                        mostrarNotificacionRecordatorio(recordatorio.nombreActividad, recordatorio.deNombre)
                        runCatching { compartirSyncRepository.eliminarRecordatorioSolicitado(recordatorio.id) }
                    }
                }
            }

            launch {
                // Invitaciones a Familia y Círculo de cuidado, en ambos sentidos — ver
                // `Plan/08-decisiones-tecnicas.md`. Vacío hasta que la cuenta esté vinculada.
                val correo = usuarioRepository.observarUsuario().first()?.correo ?: return@launch
                sincronizarYDetectarEventosSolicitudesUseCase(sesion.usuarioId, correo).collect { evento ->
                    mostrarNotificacionSolicitud(evento)
                }
            }
        }
    }

    private suspend fun mostrarNotificacionRecordatorio(nombreActividad: String, deNombre: String) {
        postearYRegistrar(
            emoji = "🔔",
            titulo = "$deNombre te recuerda",
            cuerpo = nombreActividad,
            claveNotificacion = "recordarle:$nombreActividad",
        )
    }

    /** Copys motivadores a propósito — la idea es que abrir una notificación de Lula se sienta
     * bien (una invitación, una bienvenida, un logro), nunca como un trámite. Ver
     * `Plan/08-decisiones-tecnicas.md`. */
    private suspend fun mostrarNotificacionSolicitud(evento: EventoSolicitud) {
        val (emoji, titulo, cuerpo) = when (evento) {
            is EventoSolicitud.NuevaRecibida -> {
                val s = evento.solicitud
                when (s.tipo) {
                    TipoSolicitud.ESPACIO -> Triple(
                        "👨‍👩‍👧",
                        "Nueva invitación de familia",
                        "${s.deNombre} te invitó a la Familia \"${s.contexto}\" — entra y únete",
                    )
                    TipoSolicitud.ACTIVIDAD -> Triple(
                        "👥",
                        "Nueva invitación",
                        "${s.deNombre} quiere que lo acompañes en \"${s.contexto}\" — entra y acepta",
                    )
                }
            }
            is EventoSolicitud.Respondida -> {
                val s = evento.solicitud
                val quien = s.nombreQuienResponde ?: s.para
                when (s.estado) {
                    EstadoSolicitud.ACEPTADA -> when (s.tipo) {
                        TipoSolicitud.ESPACIO -> Triple(
                            "✅",
                            "¡Invitación aceptada!",
                            "$quien se unió a la Familia \"${s.contexto}\" — ahora pueden avanzar juntos 🎉",
                        )
                        TipoSolicitud.ACTIVIDAD -> Triple(
                            "✅",
                            "¡Invitación aceptada!",
                            "$quien ahora te acompaña en \"${s.contexto}\" — sigue así 💪",
                        )
                    }
                    else -> Triple(
                        "👋",
                        "Invitación no aceptada",
                        "$quien no aceptó por ahora la invitación a \"${s.contexto}\" — puedes volver a intentarlo más adelante",
                    )
                }
            }
        }
        val solicitudId = when (evento) {
            is EventoSolicitud.NuevaRecibida -> evento.solicitud.id
            is EventoSolicitud.Respondida -> evento.solicitud.id
        }
        postearYRegistrar(emoji, titulo, cuerpo, claveNotificacion = "solicitud:$solicitudId", solicitudId = solicitudId)
    }

    /** Único lugar que posta al sistema Y guarda en el historial permanente (`notificacion`) —
     * así ningún aviso nuevo se puede olvidar de quedar registrado. Ver
     * `Plan/08-decisiones-tecnicas.md`. */
    private suspend fun postearYRegistrar(emoji: String, titulo: String, cuerpo: String, claveNotificacion: String, solicitudId: String? = null) {
        notificacionRepository.registrar(emoji, titulo, cuerpo, solicitudId)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacion = NotificationCompat.Builder(context, NotificationChannels.RECORDATORIOS_SONIDO)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("$emoji $titulo")
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        // Sin permiso POST_NOTIFICATIONS (Android 13+) esto se descarta en silencio, mismo
        // criterio que `RecordatorioReceiver.mostrarNotificacionConNivel` — el historial (arriba)
        // igual queda guardado.
        runCatching { NotificationManagerCompat.from(context).notify(claveNotificacion.hashCode(), notificacion) }
    }

    /** Botón global de escanear (barra superior, visible en toda la app) — detecta solo qué
     * tipo de código de Lula es y actúa: importa una Lista, o deja el correo de un contacto
     * listo para pegar en "Compartir"/"Invitar". Ver `Plan/12-firebase-auth-y-sync.md`. */
    fun escanear(qrTexto: String) {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            val lista = decodificarListaQr(qrTexto)
            if (lista != null) {
                importarListaDesdeQrUseCase(qrTexto, sesion.espacioId, sesion.usuarioId)
                _uiState.update { it.copy(mensaje = "Lista \"${lista.nombre}\" importada ✅") }
                return@launch
            }
            val codigoEspacio = decodificarCodigoEspacioQr(qrTexto)
            if (codigoEspacio != null) {
                val resultado = unirseAEspacioConCodigoUseCase(codigoEspacio.codigoId, sesion.usuarioId)
                _uiState.update {
                    it.copy(
                        mensaje = when (resultado) {
                            is ResultadoUnionEspacio.Exito -> "Te uniste a \"${resultado.nombreEspacio}\" ✅"
                            ResultadoUnionEspacio.CodigoInvalido -> "Ese código ya venció — pídele que muestre uno nuevo"
                        },
                    )
                }
                return@launch
            }
            val codigoCompartir = decodificarCodigoCompartirQr(qrTexto)
            if (codigoCompartir != null) {
                val resultado = reclamarCodigoCompartirActividadUseCase(codigoCompartir.codigoId)
                _uiState.update {
                    it.copy(
                        mensaje = when (resultado) {
                            is ResultadoReclamoCompartir.Exito ->
                                "Ahora acompañas a ${resultado.deNombre} en \"${resultado.nombreActividad}\" ✅"
                            ResultadoReclamoCompartir.CodigoInvalido ->
                                "Ese código ya venció, o necesitas tu cuenta vinculada con Google — pídele uno nuevo"
                        },
                    )
                }
                return@launch
            }
            val contacto = decodificarContactoQr(qrTexto)
            if (contacto != null) {
                _uiState.update {
                    it.copy(
                        correoParaCopiar = contacto.correo,
                        mensaje = "Correo de ${contacto.nombre} copiado — pégalo en \"Compartir\" o \"Invitar\"",
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(mensaje = "Ese código no es de Lula") }
        }
    }

    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    fun correoCopiado() {
        _uiState.update { it.copy(correoParaCopiar = null) }
    }

    /**
     * La racha no tiene una fuente reactiva (`calcularRachaActual` es un cálculo puntual, no
     * un `Flow`) — se vuelve a pedir cada vez que cambia de pantalla (ver `LulaTopBar`), para
     * que se note después de "Cerrar mi día" sin tener que reabrir la app.
     */
    fun refrescar() {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            refrescarRacha(sesion.espacioId)
            refrescarEspacioActivo(sesion.espacioId, sesion.usuarioId)
        }
    }

    private suspend fun refrescarRacha(espacioId: String) {
        val racha = obtenerProgresoDeHoyUseCase.calcularRachaActual(espacioId)
        _uiState.update { it.copy(racha = racha) }
    }

    private suspend fun refrescarEspacioActivo(espacioId: String, usuarioId: String) {
        val personal = espacioRepository.obtenerEspacioPersonal(usuarioId)
        val esPersonal = personal == null || personal.id == espacioId
        val espacio = if (esPersonal) null else espacioRepository.obtenerEspacioSiEsMiembro(espacioId, usuarioId)
        _uiState.update { it.copy(nombreEspacioActivo = espacio?.nombre) }
        actualizarSincronizacionEspacio(espacioId, espacio?.tipo, usuarioId)
    }

    /** Sync de contenido de Espacio Familia (paso 5, `Plan/12-firebase-auth-y-sync.md`) — corre
     * mientras ese espacio siga siendo el activo; se cancela sola al cambiar de espacio o cerrar
     * la app (el listener de Firestore vive dentro de este `Job`). */
    private fun actualizarSincronizacionEspacio(espacioId: String, tipo: TipoEspacio?, usuarioId: String) {
        if (espacioId == espacioIdSincronizado) return
        jobSincronizacionEspacio?.cancel()
        espacioIdSincronizado = espacioId
        if (tipo != TipoEspacio.FAMILIA) return
        jobSincronizacionEspacio = viewModelScope.launch {
            runCatching { sincronizarEspacioFamiliaUseCase(espacioId, usuarioId) }
        }
    }
}
