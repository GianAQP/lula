package com.aqpseller.lulaapp.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Pide el ID token de Google vía Credential Manager (reemplazo moderno de la API vieja
 * `com.google.android.gms:play-services-auth`). Ver `Plan/12-firebase-auth-y-sync.md`.
 *
 * Lanza `androidx.credentials.exceptions.GetCredentialException` si la persona cancela el
 * selector o no hay ninguna cuenta de Google disponible — se maneja en la UI que la llama.
 */
suspend fun obtenerGoogleIdToken(context: Context, serverClientId: String): String {
    val opcion = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(opcion)
        .build()

    val resultado = CredentialManager.create(context).getCredential(context, request)
    val credential = resultado.credential

    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Tipo de credencial inesperado: ${credential.type}"
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
