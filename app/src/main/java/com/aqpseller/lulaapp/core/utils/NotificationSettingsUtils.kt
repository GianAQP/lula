package com.aqpseller.lulaapp.core.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * Lleva al usuario a los ajustes nativos de notificaciones de Lula, donde puede personalizar
 * el sonido de cada canal (Silencioso/Sonido/Alarma) — Android no permite cambiar el sonido
 * de un canal ya creado desde código, solo desde Ajustes. Ver `Plan/08-decisiones-tecnicas.md`.
 */
fun abrirAjustesDeNotificaciones(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

/**
 * `POST_NOTIFICATIONS` solo existe desde Android 13 — antes de eso siempre está "concedido".
 * Android no vuelve a mostrar el diálogo de sistema después de una negación — la única forma
 * de "reintentar" es llevar al usuario a Ajustes (`abrirAjustesDeNotificaciones`).
 * `SettingsScreen` re-revisa este estado al volver a primer plano (`ON_RESUME`) para saber si
 * mostrar ese atajo.
 */
fun notificacionesPermitidas(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

/** `canScheduleExactAlarms()` solo existe desde Android 12 — antes de eso siempre está "permitido". */
fun alarmasExactasPermitidas(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() ?: true
}

/**
 * `RecordatorioScheduler` ya cae solo a una alarma inexacta si no hay permiso (nunca falla),
 * pero eso puede atrasar un recordatorio varios minutos — este atajo lleva directo al toggle
 * de "Alarmas y recordatorios" de Lula en Ajustes del sistema, sin que el usuario tenga que
 * buscarlo.
 */
fun abrirAjustesDeAlarmasExactas(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

/**
 * Varios fabricantes (Motorola incluido) matan procesos en segundo plano de forma agresiva por
 * defecto — eso puede cortar el sonido de una Alarma a mitad de reproducción, o directo evitar
 * que suene, sin que quede ningún error visible (ver el bug reportado por el usuario,
 * `08-decisiones-tecnicas.md`). Esta excepción es exactamente lo que Play Store permite pedir
 * para apps cuya función principal depende de alarmas/recordatorios confiables.
 */
fun exentoDeOptimizacionBateria(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    return context.getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

fun abrirAjustesDeOptimizacionBateria(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
