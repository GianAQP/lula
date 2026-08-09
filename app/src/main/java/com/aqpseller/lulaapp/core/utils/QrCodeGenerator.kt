package com.aqpseller.lulaapp.core.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Genera un código QR 100% local (sin red, sin servidor) — útil para transmitir un texto de
 * invitación a otro teléfono con solo acercar la cámara, en vez de escribirlo a mano.
 */
object QrCodeGenerator {
    fun generar(texto: String, tamanoPx: Int = 512): ImageBitmap? = runCatching {
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx)
        val bitmap = Bitmap.createBitmap(tamanoPx, tamanoPx, Bitmap.Config.RGB_565)
        for (x in 0 until tamanoPx) {
            for (y in 0 until tamanoPx) {
                bitmap.setPixel(x, y, if (matriz[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    }.getOrNull()
}
