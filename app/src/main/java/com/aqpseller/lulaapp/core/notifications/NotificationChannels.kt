package com.aqpseller.lulaapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.aqpseller.lulaapp.R
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio

/**
 * 3 canales — uno por nivel de recordatorio (`NivelRecordatorio`), elegible por el usuario en
 * cada Hábito/Tarea. El sonido/importancia de un canal es responsabilidad de Android una vez
 * creado (la app no puede cambiarlo por código después) — por eso son 3 canales separados
 * desde el día 1, en vez de uno solo cuyo sonido no se pudiera ajustar por nivel más adelante.
 * Ver `Plan/08-decisiones-tecnicas.md`.
 *
 * Sonido usa el sonido propio de Lula (`res/raw`), no el tono del sistema — como Android nunca
 * deja cambiar el sonido de un canal ya creado, su ID lleva sufijo `_v2`: instalaciones que ya
 * habían creado el canal original (con el tono de sistema) reciben un canal nuevo con el
 * sonido correcto, en vez de quedarse pegadas al sonido viejo para siempre.
 *
 * Alarma **no** lleva sonido en el canal (`setSound(null, null)`) — un `NotificationChannel`
 * solo puede reproducir su sonido una vez por notificación, y Alarma necesita sonar en loop
 * hasta que la persona la corte, así que ese control lo tiene `AlarmaSonidoService` con su
 * propio `MediaPlayer`, no el canal. Por eso su ID ya va en `_v4` (pasó por tener el tono de
 * sistema, después el sonido propio de Lula una sola vez, después sin sonido de canal — y ahora
 * de nuevo, porque en un dispositivo real terminó con un sonido asignado a mano desde Ajustes
 * del sistema, `mUserLockedFields` en `dumpsys notification` lo confirmó — Android bloquea ese
 * campo para la app apenas alguien lo toca desde Ajustes, así que la única forma de "limpiarlo"
 * es, otra vez, un canal nuevo). Ver `Plan/08-decisiones-tecnicas.md`.
 * Los canales viejos se borran para no dejar basura en Ajustes del sistema.
 */
object NotificationChannels {
    const val RECORDATORIOS_SILENCIOSO = "recordatorios_silencioso"
    const val RECORDATORIOS_SONIDO = "recordatorios_sonido_v2"
    const val RECORDATORIOS_ALARMA = "recordatorios_alarma_v4"

    private val CANALES_HUERFANOS = listOf(
        "recordatorios_sonido",
        "recordatorios_alarma",
        "recordatorios_alarma_v2",
        "recordatorios_alarma_v3",
    )

    private fun uriRecursoRaw(context: Context, resId: Int): Uri =
        Uri.parse("android.resource://${context.packageName}/$resId")

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        CANALES_HUERFANOS.forEach { manager.deleteNotificationChannel(it) }

        val silencioso = NotificationChannel(
            RECORDATORIOS_SILENCIOSO,
            "Recordatorios silenciosos",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Solo aparecen en la barra de notificaciones, sin sonido."
            setSound(null, null)
        }

        val atributosSonido = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sonido = NotificationChannel(
            RECORDATORIOS_SONIDO,
            "Recordatorios con sonido",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notificación llamativa con el sonido propio de Lula — el nivel por defecto."
            setSound(uriRecursoRaw(context, R.raw.lula_mensaje), atributosSonido)
        }

        val alarma = NotificationChannel(
            RECORDATORIOS_ALARMA,
            "Recordatorios tipo alarma",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "El nivel más insistente: aviso a pantalla completa, suena en loop hasta que lo detienes."
            enableVibration(true)
            // Sin sonido propio del canal a propósito: lo controla AlarmaSonidoService (loop).
            setSound(null, null)
        }

        manager.createNotificationChannel(silencioso)
        manager.createNotificationChannel(sonido)
        manager.createNotificationChannel(alarma)
    }

    fun canalPara(nivel: NivelRecordatorio): String = when (nivel) {
        NivelRecordatorio.SILENCIOSO -> RECORDATORIOS_SILENCIOSO
        NivelRecordatorio.SONIDO -> RECORDATORIOS_SONIDO
        NivelRecordatorio.ALARMA -> RECORDATORIOS_ALARMA
    }
}
