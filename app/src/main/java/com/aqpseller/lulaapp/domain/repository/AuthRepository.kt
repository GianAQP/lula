package com.aqpseller.lulaapp.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Interfaz estable: no cambia cuando se conecte Firebase Auth, solo su implementación
 * (ver `Plan/08-decisiones-tecnicas.md`, sección "usuario semilla").
 */
interface AuthRepository {
    fun observarUsuarioActualId(): Flow<String?>
    suspend fun usuarioActualId(): String?
    suspend fun signOut()
}
