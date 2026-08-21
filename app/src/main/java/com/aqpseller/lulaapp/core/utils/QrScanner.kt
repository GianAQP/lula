package com.aqpseller.lulaapp.core.utils

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.tasks.await

/**
 * Escanea un código QR con el escáner de Google Play Services (sin pedir permiso de cámara
 * propio ni construir una pantalla de cámara — Play Services se encarga). Devuelve el texto
 * crudo del QR, o `null` si la persona canceló o no se pudo leer. Ver
 * `Plan/12-firebase-auth-y-sync.md`.
 */
suspend fun escanearQr(context: Context): String? {
    val opciones = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    return runCatching {
        GmsBarcodeScanning.getClient(context, opciones).startScan().await().rawValue
    }.getOrNull()
}
