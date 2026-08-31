package com.aqpseller.lulaapp.core.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.aqpseller.lulaapp.R
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hace sonar el nivel Alarma en loop hasta que la persona lo corte — a diferencia de
 * Sonido/Silencioso, que suenan una sola vez con el sonido del propio canal
 * (`NotificationChannels`), Alarma necesita repetirse indefinidamente, y un `NotificationChannel`
 * de Android solo puede reproducir su sonido una vez por notificación. Por eso el canal Alarma
 * no lleva sonido propio (ver `NotificationChannels`) y este Service, arrancado en paralelo por
 * `RecordatorioReceiver`, es quien controla el loop con un `MediaPlayer` propio.
 *
 * Se puede cortar de 4 formas, las primeras 3 terminan en `ACTION_DETENER`: el botón
 * "🔕 Detener alarma" de la notificación, tocar la notificación para abrir la app
 * (`MainActivity`), deslizarla para descartarla (`setDeleteIntent`), o sola, si el usuario
 * configuró una duración máxima en Ajustes (a pedido del usuario, mismo concepto que "Silenciar
 * después de" del reloj nativo — antes sonaba en loop sin límite hasta apagarla a mano). Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
@AndroidEntryPoint
class AlarmaSonidoService : Service() {

    @Inject lateinit var ajustesRepository: AjustesRepository

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var jobDuracionMaxima: Job? = null
    private var ultimoStartId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val claveNotificacion = intent?.getIntExtra(EXTRA_CLAVE_NOTIFICACION, 0) ?: 0
        Log.i(TAG, "onStartCommand startId=$startId accion=${intent?.action} clave=$claveNotificacion mediaPlayerActivo=${mediaPlayer != null}")
        ultimoStartId = startId
        if (intent?.action == ACTION_DETENER) {
            detener(claveNotificacion)
        } else {
            iniciar(claveNotificacion)
        }
        return START_NOT_STICKY
    }

    /** `@Synchronized` a propósito: si Android llega a entregar el disparo de una misma alarma
     * dos veces (redelivery de `AlarmManager`, más probable bajo presión de CPU/batería como una
     * videollamada activa — caso real reportado por el usuario, donde el botón "Detener" pareció
     * funcionar pero el sonido siguió), sin esto dos `onStartCommand` casi simultáneos podían
     * pisarse el campo `mediaPlayer` entre sí y dejar un reproductor huérfano sonando sin que
     * ninguna notificación visible ya apuntara a él. Ver `Plan/08-decisiones-tecnicas.md`. */
    @Synchronized
    private fun iniciar(claveNotificacion: Int) {
        detenerReproduccion()
        val notificacionServicio = NotificationCompat.Builder(this, NotificationChannels.RECORDATORIOS_ALARMA)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 Alarma sonando")
            .setContentText("Toca \"Detener\" cuando la veas.")
            .addAction(0, "🔕 Detener alarma", pendingIntentDetener(claveNotificacion))
            .setOngoing(true)
            .build()
        startForeground(ID_NOTIFICACION_SERVICIO, notificacionServicio)

        // Un Service en primer plano normalmente no necesita esto — pero con la pantalla apagada
        // y el teléfono inactivo, la CPU puede volver a dormirse a mitad de la reproducción del
        // `MediaPlayer` (nada más lo mantenía despierto) y el sonido se cortaba en menos de un
        // segundo, exactamente el bug reportado por el usuario. Se libera solo, como máximo, a
        // los 2 minutos — más que de sobra para que termine de sonar y el usuario alcance a
        // tocar "Detener"; después de eso ya no hace falta forzar la CPU despierta. Ver
        // `Plan/08-decisiones-tecnicas.md`.
        runCatching {
            val powerManager = getSystemService<PowerManager>()
            wakeLock = powerManager
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LulaApp:AlarmaSonidoWakeLock")
                ?.apply { acquire(2 * 60_000L) }
        }.onFailure { error ->
            Log.e(TAG, "No se pudo tomar el WakeLock de la alarma", error)
        }

        val atributosAlarma = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        solicitarAudioFocus(atributosAlarma)
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(atributosAlarma)
                setDataSource(this@AlarmaSonidoService, Uri.parse("android.resource://$packageName/${R.raw.lula_alarma_gorrion_habla_ventana}"))
                isLooping = true
                // Sin esto, un error asíncrono después de `start()` (ej. el decoder se cae a
                // mitad de reproducción) dejaba la alarma sonando "menos de un segundo y
                // cortada", sin loguear nada — pasaba desapercibido. Ver `08-decisiones-tecnicas.md`.
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error de la alarma: what=$what extra=$extra")
                    true
                }
                prepare()
                start()
            }
        }.onFailure { error ->
            // Antes esto se tragaba en silencio (`runCatching` sin rama de error) — si fallaba,
            // no había forma de enterarse; ahora al menos queda en Logcat para diagnosticar.
            Log.e(TAG, "No se pudo iniciar el sonido de la alarma", error)
        }

        jobDuracionMaxima?.cancel()
        jobDuracionMaxima = serviceScope.launch {
            val minutos = ajustesRepository.observarDuracionMaximaAlarmaMin().first()
            if (minutos != null) {
                delay(minutos * 60_000L)
                detener(claveNotificacion)
            }
        }
    }

    private fun solicitarAudioFocus(atributos: AudioAttributes) {
        val audioManager = getSystemService<AudioManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(atributos)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonarAudioFocus() {
        val audioManager = getSystemService<AudioManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        audioFocusRequest = null
    }

    @Synchronized
    private fun detener(claveNotificacion: Int) {
        Log.i(TAG, "detener() clave=$claveNotificacion mediaPlayerActivo=${mediaPlayer != null}")
        jobDuracionMaxima?.cancel()
        jobDuracionMaxima = null
        detenerReproduccion()
        if (claveNotificacion != 0) {
            NotificationManagerCompat.from(this).cancel(claveNotificacion)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        // `stopSelf(startId)` en vez de `stopSelf()` — la versión sin argumentos detiene el
        // Service sin importar si ya llegó un `onStartCommand` más nuevo mientras tanto (ej. un
        // segundo disparo de la misma alarma); con `startId` solo se detiene si este sigue
        // siendo el último comando recibido.
        stopSelf(ultimoStartId)
    }

    @Synchronized
    private fun detenerReproduccion() {
        mediaPlayer?.let { player ->
            runCatching { player.stop() }.onFailure { Log.e(TAG, "Error al detener el MediaPlayer de la alarma", it) }
            runCatching { player.release() }.onFailure { Log.e(TAG, "Error al liberar el MediaPlayer de la alarma", it) }
        }
        mediaPlayer = null
        abandonarAudioFocus()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        jobDuracionMaxima?.cancel()
        serviceScope.cancel()
        detenerReproduccion()
        super.onDestroy()
    }

    private fun pendingIntentDetener(claveNotificacion: Int) =
        PendingIntent.getService(
            this,
            claveNotificacion,
            intentDetener(this, claveNotificacion),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_DETENER = "com.aqpseller.lulaapp.ACTION_DETENER_ALARMA"
        const val EXTRA_CLAVE_NOTIFICACION = "claveNotificacion"
        private const val ID_NOTIFICACION_SERVICIO = 999_001
        private const val TAG = "AlarmaSonidoService"

        fun intentDetener(context: Context, claveNotificacion: Int): Intent =
            Intent(context, AlarmaSonidoService::class.java).apply {
                action = ACTION_DETENER
                putExtra(EXTRA_CLAVE_NOTIFICACION, claveNotificacion)
            }
    }
}
