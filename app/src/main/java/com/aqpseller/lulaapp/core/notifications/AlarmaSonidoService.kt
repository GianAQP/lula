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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.aqpseller.lulaapp.R

/**
 * Hace sonar el nivel Alarma en loop hasta que la persona lo corte — a diferencia de
 * Sonido/Silencioso, que suenan una sola vez con el sonido del propio canal
 * (`NotificationChannels`), Alarma necesita repetirse indefinidamente, y un `NotificationChannel`
 * de Android solo puede reproducir su sonido una vez por notificación. Por eso el canal Alarma
 * no lleva sonido propio (ver `NotificationChannels`) y este Service, arrancado en paralelo por
 * `RecordatorioReceiver`, es quien controla el loop con un `MediaPlayer` propio.
 *
 * Se puede cortar de 3 formas, todas terminan en `ACTION_DETENER`: el botón "🔕 Detener alarma"
 * de la notificación, tocar la notificación para abrir la app (`MainActivity`), o deslizarla
 * para descartarla (`setDeleteIntent`).
 */
class AlarmaSonidoService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val claveNotificacion = intent?.getIntExtra(EXTRA_CLAVE_NOTIFICACION, 0) ?: 0
        if (intent?.action == ACTION_DETENER) {
            detener(claveNotificacion)
        } else {
            iniciar(claveNotificacion)
        }
        return START_NOT_STICKY
    }

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

    private fun detener(claveNotificacion: Int) {
        detenerReproduccion()
        if (claveNotificacion != 0) {
            NotificationManagerCompat.from(this).cancel(claveNotificacion)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun detenerReproduccion() {
        mediaPlayer?.let { runCatching { it.stop() }; it.release() }
        mediaPlayer = null
        abandonarAudioFocus()
    }

    override fun onDestroy() {
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
