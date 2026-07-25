package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.UsuarioDao
import com.aqpseller.lulaapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementación local (sin Firebase) — siempre devuelve el único `UsuarioEntity` existente.
 * No conoce Firebase; cuando se conecte, esta clase se reemplaza por
 * `AuthRepositoryFirebaseImpl` sin tocar `AuthRepository` ni ningún caso de uso/pantalla
 * (ver `Plan/08-decisiones-tecnicas.md`).
 */
class AuthRepositoryLocalImpl @Inject constructor(
    private val usuarioDao: UsuarioDao,
) : AuthRepository {

    override fun observarUsuarioActualId(): Flow<String?> =
        usuarioDao.observarUnico().map { it?.id }

    override suspend fun usuarioActualId(): String? = usuarioDao.obtenerUnico()?.id

    override suspend fun signOut() {
        // No aplica sin autenticación real: el usuario semilla local persiste siempre.
    }
}
