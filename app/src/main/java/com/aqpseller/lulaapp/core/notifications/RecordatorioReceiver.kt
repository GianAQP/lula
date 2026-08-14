package com.aqpseller.lulaapp.core.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aqpseller.lulaapp.MainActivity
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.horariosParaFecha
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.model.Recurrencia
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import com.aqpseller.lulaapp.navigation.LulaDestinations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import javax.inject.Inject

/**
 * Recibe la alarma programada por `RecordatorioScheduler` y muestra la notificación según el
 * nivel elegido por el usuario (`NivelRecordatorio`). Si es de un Hábito o una toma de
 * Medicamento (recurrentes), se reprograma a sí mismo para el día siguiente a la misma hora —
 * así no depende de `setRepeating` (inexacto y con drift en Android moderno). Tarea y Cita son
 * de un solo disparo, no se reprograman.
 */
@AndroidEntryPoint
class RecordatorioReceiver : BroadcastReceiver() {

    @Inject lateinit var recordatorioScheduler: RecordatorioScheduler

    @Inject lateinit var actividadRepository: ActividadRepository

    @Inject lateinit var obtenerSesionActualUseCase: ObtenerSesionActualUseCase

    @Inject lateinit var obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_ES_RECORDATORIO_CIERRE_DIA, false)) {
            manejarRecordatorioCierreDia(context, intent.getStringExtra(EXTRA_HORA) ?: "20:00")
            return
        }
        intent.getStringExtra(EXTRA_MOMENTO_FRANJA)?.let { momentoNombre ->
            val momento = runCatching { MomentoDelDia.valueOf(momentoNombre) }.getOrNull() ?: return
            manejarRecordatorioFranja(context, momento, intent.getStringExtra(EXTRA_HORA) ?: "09:00")
            return
        }
        intent.getStringExtra(EXTRA_META_ID)?.let { metaId ->
            val nivelMeta = intent.getStringExtra(EXTRA_NIVEL)?.let { runCatching { NivelRecordatorio.valueOf(it) }.getOrNull() }
                ?: NivelRecordatorio.SONIDO
            mostrarNotificacionMeta(context, metaId, intent.getStringExtra(EXTRA_NOMBRE).orEmpty(), nivelMeta)
            return
        }
        val actividadId = intent.getStringExtra(EXTRA_ACTIVIDAD_ID) ?: return
        val nombre = intent.getStringExtra(EXTRA_NOMBRE).orEmpty()
        val tipo = intent.getStringExtra(EXTRA_TIPO)?.let { runCatching { TipoActividad.valueOf(it) }.getOrNull() }
            ?: TipoActividad.TAREA
        val hora = intent.getStringExtra(EXTRA_HORA)
        val horario = intent.getStringExtra(EXTRA_HORARIO)
        val instruccion = intent.getStringExtra(EXTRA_INSTRUCCION).orEmpty()
        val nivel = intent.getStringExtra(EXTRA_NIVEL)?.let { runCatching { NivelRecordatorio.valueOf(it) }.getOrNull() }
            ?: NivelRecordatorio.SONIDO
        val esRenotificacion = intent.getBooleanExtra(EXTRA_ES_RENOTIFICACION, false)
        val intervaloPersistenciaMin = intent.getIntExtra(EXTRA_INTERVALO_PERSISTENCIA_MIN, -1).takeIf { it > 0 }

        // Insistencia del recordatorio persistente — es una alarma aparte de la diaria normal,
        // decide sola (según si la toma sigue sin marcar) si se muestra de nuevo y se reprograma,
        // o si ya no hace falta seguir. No pasa por el camino normal de abajo.
        if (esRenotificacion && tipo == TipoActividad.MEDICAMENTO && horario != null && intervaloPersistenciaMin != null) {
            val fechaOriginal = intent.getLongExtra(EXTRA_FECHA_ORIGINAL_EPOCH_DAY, -1L)
            if (fechaOriginal >= 0) {
                manejarRenotificacionMedicamento(context, actividadId, nombre, horario, instruccion, nivel, intervaloPersistenciaMin, fechaOriginal)
            }
            return
        }

        mostrarNotificacion(context, actividadId, nombre, tipo, nivel, horario, instruccion)

        when {
            tipo == TipoActividad.HABITO && hora != null ->
                recordatorioScheduler.programarHabito(actividadId, nombre, hora, nivel)
            tipo == TipoActividad.MEDICAMENTO && horario != null -> {
                reprogramarMedicamentoSiVigente(actividadId, nombre, horario, instruccion, nivel)
                if (intervaloPersistenciaMin != null) {
                    recordatorioScheduler.programarRenotificacionMedicamento(
                        actividadId,
                        nombre,
                        horario,
                        instruccion,
                        nivel,
                        intervaloPersistenciaMin,
                        fechaOriginalEpochDay = DateTimeUtils.hoy().toEpochDays().toLong(),
                    )
                }
            }
            tipo == TipoActividad.FECHA_IMPORTANTE ->
                reprogramarFechaImportanteSiRepite(actividadId, nombre)
        }
    }

    /**
     * Recordatorio diario genérico — se salta la notificación (pero se sigue reprogramando
     * para mañana) si el día ya se cerró, para no insistir con algo que ya se hizo. No usa
     * `mostrarNotificacion` porque ese método arma el texto a partir de un `TipoActividad` real
     * (Hábito/Tarea/etc.), y este recordatorio no es de ningún tipo en particular.
     */
    private fun manejarRecordatorioCierreDia(context: Context, hora: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val sesion = obtenerSesionActualUseCase()
                val yaCerrado = obtenerProgresoDeHoyUseCase.registroDeHoy(sesion.espacioId) != null
                if (!yaCerrado) {
                    mostrarNotificacionCierreDia(context)
                }
                recordatorioScheduler.programarRecordatorioCierreDia(hora)
            }
            pendingResult.finish()
        }
    }

    /**
     * Recordatorio genérico "revisa Lula" por franja del día — a diferencia del de cierre del
     * día, no depende de ningún registro: solo avisa a la hora elegida, todos los días, mismo
     * patrón de auto-reprogramación que un Hábito.
     */
    private fun manejarRecordatorioFranja(context: Context, momento: MomentoDelDia, hora: String) {
        mostrarNotificacionFranja(context, momento)
        recordatorioScheduler.programarRecordatorioFranja(momento, hora)
    }

    private fun mostrarNotificacionFranja(context: Context, momento: MomentoDelDia) {
        val claveNotificacion = "recordatorio_franja_${momento.name}".hashCode()
        val intentContenido = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DESTINO, LulaDestinations.HOY)
        }
        val pendingIntentContenido = PendingIntent.getActivity(
            context, claveNotificacion, intentContenido, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val etiquetaMomento = when (momento) {
            MomentoDelDia.MANANA -> "la mañana"
            MomentoDelDia.TARDE -> "la tarde"
            MomentoDelDia.NOCHE -> "la noche"
        }
        val builder = NotificationCompat.Builder(context, NotificationChannels.RECORDATORIOS_SONIDO)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 ¿Revisaste Lula esta $etiquetaMomento?")
            .setContentText("Dale un vistazo a lo que tienes pendiente hoy.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntentContenido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        runCatching {
            NotificationManagerCompat.from(context).notify(claveNotificacion, builder.build())
        }.onFailure { error ->
            Log.e("RecordatorioReceiver", "No se pudo mostrar el recordatorio de franja ($momento)", error)
        }
    }

    /**
     * Meta vive en una tabla completamente aparte de `Actividad` — no pasa por el camino normal
     * de `mostrarNotificacion` (que espera un `TipoActividad` real) ni se reprograma sola, la
     * fecha límite de una meta no se repite. Un solo disparo, pero SÍ respeta el nivel elegido
     * (Silencioso/Sonido/Alarma, igual que el resto de la app) vía `mostrarNotificacionConNivel`.
     * Ver `Plan/08-decisiones-tecnicas.md`.
     */
    private fun mostrarNotificacionMeta(context: Context, metaId: String, nombre: String, nivel: NivelRecordatorio) {
        val claveNotificacion = "recordatorio_meta_$metaId".hashCode()
        val cuerpo = nombre.ifBlank { "Tienes una meta pendiente en Lula" }
        mostrarNotificacionConNivel(
            context, claveNotificacion, LulaDestinations.metaDetalle(metaId), "🎯 ¡Hoy vence tu meta!", cuerpo, nivel, "metaId=$metaId",
        )
    }

    private fun mostrarNotificacionCierreDia(context: Context) {
        val claveNotificacion = "cierre_dia".hashCode()
        val intentContenido = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DESTINO, LulaDestinations.cerrarDia())
        }
        val pendingIntentContenido = PendingIntent.getActivity(
            context, claveNotificacion, intentContenido, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, NotificationChannels.RECORDATORIOS_SONIDO)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔥 ¿Cómo te fue hoy?")
            .setContentText("Cierra tu día para no perder tu racha — cada intento cuenta.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntentContenido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        runCatching { NotificationManagerCompat.from(context).notify(claveNotificacion, builder.build()) }
    }

    /**
     * Insiste cada [intervaloMin] minutos hasta que la toma se marque (tomada u omitida) o
     * cambie el día — a propósito no tiene un límite de reintentos, la persona puede tardar
     * horas en marcarla y sigue teniendo sentido insistir, pero cruzar la medianoche sí corta
     * la cadena para no encimarse con la agenda del día siguiente.
     */
    private fun manejarRenotificacionMedicamento(
        context: Context,
        actividadId: String,
        nombre: String,
        horario: String,
        instruccion: String,
        nivel: NivelRecordatorio,
        intervaloMin: Int,
        fechaOriginalEpochDay: Long,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val hoyEpochDay = DateTimeUtils.hoy().toEpochDays().toLong()
                if (hoyEpochDay == fechaOriginalEpochDay) {
                    val estado = actividadRepository.obtenerEstadoToma(actividadId, fechaOriginalEpochDay, horario)
                    if (estado == null || estado == EstadoActividad.SIN_CONFIRMAR) {
                        mostrarNotificacion(context, actividadId, nombre, TipoActividad.MEDICAMENTO, nivel, horario, instruccion)
                        recordatorioScheduler.programarRenotificacionMedicamento(
                            actividadId, nombre, horario, instruccion, nivel, intervaloMin, fechaOriginalEpochDay,
                        )
                    }
                }
            }
            pendingResult.finish()
        }
    }

    /**
     * A diferencia de Hábito (que se reprograma sin condiciones, todos los días), un Medicamento
     * puede tener `fechaFin` — antes se reprogramaba para siempre sin revisarla. Se re-lee el
     * detalle porque `fechaFin`/`cantidadDosisTotal` pueden haber cambiado desde que se programó
     * esta alarma (ej. la persona editó o pausó el medicamento).
     */
    private fun reprogramarMedicamentoSiVigente(actividadId: String, nombre: String, horario: String, instruccion: String, nivel: NivelRecordatorio) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val actividad = actividadRepository.obtenerConDetalle(actividadId)
                val detalle = actividad?.detalle as? ActividadDetalle.Medicamento
                if (actividad != null && actividad.activa && detalle != null && medicamentoSigueVigenteManana(detalle, horario)) {
                    recordatorioScheduler.programarMedicamento(actividadId, nombre, horario, instruccion, nivel)
                }
            }
            pendingResult.finish()
        }
    }

    private fun medicamentoSigueVigenteManana(detalle: ActividadDetalle.Medicamento, horario: String): Boolean {
        val manana = DateTimeUtils.hoy().plus(DatePeriod(days = 1))
        return horario in horariosParaFecha(detalle, manana)
    }

    /**
     * A diferencia de Hábito/Medicamento (cuya hora fija viaja en los extras del `Intent`),
     * una Fecha importante que se repite necesita releer `recurrencia`/`fechaBase` de la base
     * — se re-lee en vez de mandarlo todo por extras para no inflar el `Intent` genérico con
     * campos que solo usa este tipo. Nunca se muta `fechaBase`, siempre se recalcula la
     * próxima ocurrencia desde el original (ver `RecordatorioScheduler`).
     */
    private fun reprogramarFechaImportanteSiRepite(actividadId: String, nombre: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val detalle = actividadRepository.obtenerConDetalle(actividadId)?.detalle as? ActividadDetalle.FechaImportante
                if (detalle != null && detalle.recurrencia != Recurrencia.UNICA) {
                    recordatorioScheduler.programarFechaImportante(
                        actividadId, nombre, detalle.fechaBase, detalle.horaNotificacion, detalle.anticipacion, detalle.recurrencia, detalle.tipoAviso,
                    )
                }
            }
            pendingResult.finish()
        }
    }

    private fun mostrarNotificacion(
        context: Context,
        actividadId: String,
        nombre: String,
        tipo: TipoActividad,
        nivel: NivelRecordatorio,
        horario: String?,
        instruccion: String,
    ) {
        // Va a una pantalla de acción rápida (Hecho/Posponer), no directo a editar — a pedido
        // del usuario, tocar la notificación no debería mandar a un formulario de edición.
        val destino = when {
            tipo == TipoActividad.MEDICAMENTO && horario != null -> LulaDestinations.accionToma(actividadId, horario)
            tipo == TipoActividad.CITA -> LulaDestinations.SALUD
            else -> LulaDestinations.recordatorioAccion(actividadId)
        }
        val claveNotificacion = (if (horario != null) "$actividadId:$horario" else actividadId).hashCode()
        val emoji = when (tipo) {
            TipoActividad.HABITO -> "🌱"
            TipoActividad.TAREA -> "📝"
            TipoActividad.MEDICAMENTO -> "💊"
            TipoActividad.CITA -> "🩺"
            TipoActividad.FECHA_IMPORTANTE -> "🎉"
            else -> "🔔"
        }
        val tipoTexto = when (tipo) {
            TipoActividad.HABITO -> "hábito"
            TipoActividad.TAREA -> "tarea"
            TipoActividad.MEDICAMENTO -> "medicamento"
            TipoActividad.CITA -> "cita"
            TipoActividad.FECHA_IMPORTANTE -> "fecha importante"
            else -> "recordatorio"
        }
        // Para Medicamento, siempre se muestra la instrucción original (ej. "después del
        // almuerzo"), no solo la hora calculada — ver `Plan/01-arquitectura.md`.
        val cuerpo = if (tipo == TipoActividad.MEDICAMENTO && instruccion.isNotBlank()) {
            "$nombre — $instruccion"
        } else {
            nombre.ifBlank { "Tienes algo pendiente en Lula" }
        }
        mostrarNotificacionConNivel(
            context, claveNotificacion, destino, "$emoji ¡Hora de tu $tipoTexto!", cuerpo, nivel, "actividadId=$actividadId",
        )
    }

    /**
     * Arma y muestra la notificación según el nivel elegido — compartido por
     * `mostrarNotificacion` (Hábito/Tarea/Medicamento/Cita/Fecha importante, con `TipoActividad`
     * real) y `mostrarNotificacionMeta` (Meta, que vive en su tabla aparte). Antes de que existiera
     * esto, cada camino tenía su propia copia de la rama de Alarma (pantalla completa +
     * `AlarmaSonidoService`) — un solo lugar evita que se vuelvan a desalinear, como ya pasó una
     * vez con el bug del `fullScreenIntent` compartido. Ver `Plan/08-decisiones-tecnicas.md`.
     */
    private fun mostrarNotificacionConNivel(
        context: Context,
        claveNotificacion: Int,
        destino: String,
        titulo: String,
        cuerpo: String,
        nivel: NivelRecordatorio,
        logTag: String,
    ) {
        val intentContenido = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_DESTINO, destino)
            if (nivel == NivelRecordatorio.ALARMA) {
                putExtra(MainActivity.EXTRA_MOSTRAR_SOBRE_BLOQUEO, true)
                putExtra(MainActivity.EXTRA_DETENER_ALARMA, claveNotificacion)
            }
        }
        val pendingIntentContenido = PendingIntent.getActivity(
            context,
            claveNotificacion,
            intentContenido,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.canalPara(nivel))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setAutoCancel(true)
            .setContentIntent(pendingIntentContenido)

        when (nivel) {
            NivelRecordatorio.SILENCIOSO -> {
                builder.setPriority(NotificationCompat.PRIORITY_LOW)
            }
            NivelRecordatorio.SONIDO -> {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            }
            NivelRecordatorio.ALARMA -> {
                val pendingIntentDetener = PendingIntent.getService(
                    context,
                    claveNotificacion,
                    AlarmaSonidoService.intentDetener(context, claveNotificacion),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                // El `fullScreenIntent` lo dispara ANDROID SOLO para mostrar la alerta sobre la
                // pantalla bloqueada — no es una acción real de la persona, así que a propósito
                // NO lleva `EXTRA_DETENER_ALARMA` (a diferencia de `pendingIntentContenido`, que
                // sí lo lleva porque tocar la notificación SÍ es una acción real). Antes ambos
                // reusaban el mismo `PendingIntent`: apenas el teléfono estaba bloqueado/inactivo,
                // Android abría `MainActivity` solo para mostrar la alerta, y esa apertura
                // automática cortaba la alarma casi al instante — exactamente "suena un poco y
                // se corta", y solo con el teléfono inactivo porque activo el sistema no dispara
                // el full-screen intent (ver `Plan/08-decisiones-tecnicas.md`).
                val intentPantallaCompleta = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_DESTINO, destino)
                    putExtra(MainActivity.EXTRA_MOSTRAR_SOBRE_BLOQUEO, true)
                }
                val pendingIntentPantallaCompleta = PendingIntent.getActivity(
                    context,
                    "$claveNotificacion:pantallaCompleta".hashCode(),
                    intentPantallaCompleta,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pendingIntentPantallaCompleta, true)
                    .setOngoing(true)
                    .addAction(0, "🔕 Detener alarma", pendingIntentDetener)
                    .setDeleteIntent(pendingIntentDetener)
            }
        }

        // Sin permiso POST_NOTIFICATIONS (Android 13+), esto no lanza excepción — el sistema
        // simplemente descarta la notificación en silencio, sin sonido ni aviso visible en
        // ningún lado (bug real reportado por el usuario: "no sonó nada, ni sonido ni alarma").
        // El log de acá es la única pista posible; el aviso real para el usuario vive en Hoy
        // (banner "🔕 Sin permiso de notificaciones") — ver `Plan/08-decisiones-tecnicas.md`.
        runCatching {
            NotificationManagerCompat.from(context).notify(claveNotificacion, builder.build())
        }.onFailure { error ->
            Log.e("RecordatorioReceiver", "No se pudo mostrar la notificación ($logTag)", error)
        }

        // El canal Alarma no lleva sonido propio (ver `NotificationChannels`) — este Service
        // es quien hace sonar el loop hasta que la persona toque "Detener". Antes esto no
        // registraba nada si fallaba (ej. el sistema lo mata apenas arranca por optimización de
        // batería del fabricante — ver "🔋 Permitir que Lula funcione siempre" en Ajustes) — un
        // "sonó un segundo y se cortó" quedaba sin ninguna pista en Logcat para diagnosticarlo.
        if (nivel == NivelRecordatorio.ALARMA) {
            val intentServicio = Intent(context, AlarmaSonidoService::class.java)
                .putExtra(AlarmaSonidoService.EXTRA_CLAVE_NOTIFICACION, claveNotificacion)
            runCatching {
                ContextCompat.startForegroundService(context, intentServicio)
            }.onFailure { error ->
                Log.e("RecordatorioReceiver", "No se pudo iniciar AlarmaSonidoService", error)
            }
        }
    }

    companion object {
        const val EXTRA_ACTIVIDAD_ID = "actividadId"
        const val EXTRA_NOMBRE = "nombre"
        const val EXTRA_TIPO = "tipo"
        const val EXTRA_HORA = "hora"
        const val EXTRA_NIVEL = "nivel"
        const val EXTRA_HORARIO = "horario"
        const val EXTRA_INSTRUCCION = "instruccion"
        const val EXTRA_ES_RENOTIFICACION = "esRenotificacion"
        const val EXTRA_INTERVALO_PERSISTENCIA_MIN = "intervaloPersistenciaMin"
        const val EXTRA_FECHA_ORIGINAL_EPOCH_DAY = "fechaOriginalEpochDay"
        const val EXTRA_ES_RECORDATORIO_CIERRE_DIA = "esRecordatorioCierreDia"
        const val EXTRA_MOMENTO_FRANJA = "momentoFranja"
        const val EXTRA_META_ID = "metaId"
    }
}
