package com.aqpseller.lulaapp.core.utils

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Sonido corto de confirmación al marcar un check — usa el generador de tonos del sistema
 * (sin bundlear un mp3), reutilizando una sola instancia mientras la app viva.
 */
object SonidoUtils {
    private val toneGenerator: ToneGenerator? by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }.getOrNull()
    }

    fun reproducirCheck() {
        runCatching { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 80) }
    }
}
