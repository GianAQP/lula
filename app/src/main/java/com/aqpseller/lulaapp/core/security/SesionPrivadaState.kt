package com.aqpseller.lulaapp.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Si la Zona Privada ya se desbloqueó en esta sesión de la app (vive en memoria — se re-bloquea
 * siempre al reabrir la app). Auto-bloqueo por inactividad: se mide el tiempo que la app pasó
 * en segundo plano (`ON_STOP`→`ON_START` de `ProcessLifecycleOwner`, no un timer corriendo todo
 * el rato) — si al volver pasaron `TIMEOUT_INACTIVIDAD_MS` o más, se re-bloquea. No se ata a
 * gestos/toques dentro de la app (mucho más simple y ya cubre el caso real: dejar el teléfono).
 */
@Singleton
class SesionPrivadaState @Inject constructor() : DefaultLifecycleObserver {

    private val _desbloqueada = MutableStateFlow(false)
    val desbloqueada: StateFlow<Boolean> = _desbloqueada.asStateFlow()

    private var momentoAlPasarASegundoPlano: Long? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun desbloquear() {
        _desbloqueada.value = true
    }

    fun bloquear() {
        _desbloqueada.value = false
    }

    override fun onStop(owner: LifecycleOwner) {
        if (_desbloqueada.value) momentoAlPasarASegundoPlano = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        val entroAlFondo = momentoAlPasarASegundoPlano ?: return
        momentoAlPasarASegundoPlano = null
        if (System.currentTimeMillis() - entroAlFondo >= TIMEOUT_INACTIVIDAD_MS) {
            bloquear()
        }
    }

    private companion object {
        /** Dentro del rango 2-5 min pedido en `Plan/02-pantallas.md`. */
        const val TIMEOUT_INACTIVIDAD_MS = 3 * 60 * 1000L
    }
}
