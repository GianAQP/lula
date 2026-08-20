package com.aqpseller.lulaapp.domain.repository

import kotlinx.coroutines.flow.Flow

/** Devuelto por `iniciarSesionConGoogle` — datos mínimos de Firebase que necesita el caso de
 * uso "reclamar cuenta" para actualizar el usuario semilla local (ver
 * `Plan/12-firebase-auth-y-sync.md`, sección 5). */
data class ResultadoSesionGoogle(val firebaseUid: String, val correo: String?)

/**
 * Interfaz estable: no cambia cuando se conecte Firebase Auth, solo su implementación
 * (ver `Plan/08-decisiones-tecnicas.md`, sección "usuario semilla").
 */
interface AuthRepository {
    fun observarUsuarioActualId(): Flow<String?>
    suspend fun usuarioActualId(): String?
    suspend fun signOut()

    /** true si hay una sesión de Firebase activa (cuenta ya reclamada y con sesión vigente),
     * false si sigue siendo solo el usuario semilla local. */
    fun sesionFirebaseActiva(): Boolean

    /** Intercambia el ID token de Google (obtenido vía Credential Manager en la UI) por una
     * sesión real de Firebase Auth. */
    suspend fun iniciarSesionConGoogle(idToken: String): ResultadoSesionGoogle
}
