package com.aqpseller.lulaapp.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun biometriaDisponible(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS

fun mostrarPromptBiometrico(
    activity: FragmentActivity,
    onExito: () -> Unit,
    onError: () -> Unit,
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Confirma que eres tú")
        .setSubtitle("Zona privada de Lula")
        .setNegativeButtonText("Usar PIN en su lugar")
        .build()

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onExito()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError()
            }
        },
    )
    prompt.authenticate(promptInfo)
}
