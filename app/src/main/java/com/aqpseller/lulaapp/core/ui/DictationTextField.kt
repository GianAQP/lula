package com.aqpseller.lulaapp.core.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/**
 * `OutlinedTextField` con un botón de dictado 🎤 que delega la captura de voz al reconocedor
 * del sistema (`RecognizerIntent`) — no requiere pedir permiso de micrófono en la app, lo
 * maneja la app de reconocimiento. "Dictado de campo": transcribe, no interpreta intención
 * (ver `Plan/03-vocabulario.md`). Usar en todo campo de texto libre desde el MVP 0.1.
 */
@Composable
fun DictationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == Activity.RESULT_OK) {
            val textoDictado = resultado.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!textoDictado.isNullOrBlank()) {
                onValueChange(if (value.isBlank()) textoDictado else "$value $textoDictado")
            } else {
                Toast.makeText(context, "No entendí lo que dijiste, prueba de nuevo", Toast.LENGTH_SHORT).show()
            }
        } else if (resultado.resultCode != Activity.RESULT_CANCELED) {
            // RESULT_CANCELED es normal (el usuario cerró el diálogo de dictado a propósito) —
            // cualquier otro código es una falla real que antes quedaba silenciosa.
            Toast.makeText(context, "El dictado no funcionó en este intento, prueba de nuevo", Toast.LENGTH_SHORT).show()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        trailingIcon = {
            IconButton(onClick = {
                // Con la pantalla bloqueada (huella/PIN sin ingresar todavía, ej. al volver de
                // una notificación con el teléfono en reposo), el reconocedor de voz del sistema
                // no llega a abrirse — antes eso se veía como el ícono "parpadeando" sin pasar
                // nada, sin ninguna pista de qué hacer. Se detecta antes de intentar lanzar.
                val keyguardManager = context.getSystemService<KeyguardManager>()
                if (keyguardManager?.isKeyguardLocked == true) {
                    Toast.makeText(context, "Desbloquea la pantalla para usar el dictado", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictar $label")
                    // NO usar EXTRA_PREFER_OFFLINE: fuerza un modelo de voz descargado en el
                    // dispositivo que la mayoría de teléfonos no tiene instalado — en vez de
                    // caer al reconocimiento en línea, el reconocedor falla de inmediato con
                    // "la búsqueda por voz no está disponible" incluso con internet andando.
                }
                runCatching { launcher.launch(intent) }
                    .onFailure { Toast.makeText(context, "Dictado no disponible en este dispositivo", Toast.LENGTH_SHORT).show() }
            }) {
                Text("🎤")
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
