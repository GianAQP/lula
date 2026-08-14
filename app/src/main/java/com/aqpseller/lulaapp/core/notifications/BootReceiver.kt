package com.aqpseller.lulaapp.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aqpseller.lulaapp.domain.usecase.notificaciones.ReprogramarTodosLosRecordatoriosUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `AlarmManager` borra todas las alarmas pendientes al reiniciar el dispositivo — sin esto,
 * los recordatorios dejarían de sonar en silencio tras cada reinicio, un bug invisible que
 * nadie notaría hasta extrañar el recordatorio. Vuelve a programar todo lo activo con hora.
 * La misma lógica también se dispara al abrir la app (`AppViewModel`) — ver
 * `ReprogramarTodosLosRecordatoriosUseCase`.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reprogramarTodosLosRecordatoriosUseCase: ReprogramarTodosLosRecordatoriosUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { reprogramarTodosLosRecordatoriosUseCase() }
            pendingResult.finish()
        }
    }
}
