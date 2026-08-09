package com.aqpseller.lulaapp.core.utils

import android.content.Context
import android.content.Intent

/**
 * Reinicia el proceso completo de la app — necesario después de `eliminarCuenta()` porque
 * ViewModels ya en memoria pueden tener cacheado el `usuarioId`/`espacioId` de filas que
 * `clearAllTables()` acaba de borrar. Matar el proceso fuerza a que todo (incluida la semilla
 * vía `AsegurarDatosSemillaUseCase`, disparada por `AppViewModel` al arrancar) se reconstruya
 * desde cero.
 */
fun reiniciarApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}
