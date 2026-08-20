package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.UsuarioDao
import com.aqpseller.lulaapp.domain.repository.AuthRepository
import com.aqpseller.lulaapp.domain.repository.ResultadoSesionGoogle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Reemplaza a la implementación local (ver `Plan/12-firebase-auth-y-sync.md`). El usuario
 * "actual" para el resto de la app SIGUE siendo el único `UsuarioEntity` local — Lula es
 * local-first, un usuario por dispositivo, con o sin cuenta reclamada. Lo nuevo acá es la
 * sesión real de Firebase, usada solo para sincronizar Familia/Círculo de cuidado.
 */
class AuthRepositoryFirebaseImpl @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override fun observarUsuarioActualId(): Flow<String?> =
        usuarioDao.observarUnico().map { it?.id }

    override suspend fun usuarioActualId(): String? = usuarioDao.obtenerUnico()?.id

    override suspend fun signOut() {
        // Solo cierra la sesión de Firebase (para de sincronizar). El usuario semilla local
        // sigue existiendo y usable sin conexión — no se borra nada acá.
        firebaseAuth.signOut()
    }

    override fun sesionFirebaseActiva(): Boolean = firebaseAuth.currentUser != null

    override suspend fun iniciarSesionConGoogle(idToken: String): ResultadoSesionGoogle {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val resultado = firebaseAuth.signInWithCredential(credential).await()
        val firebaseUser = requireNotNull(resultado.user) { "Firebase no devolvió un usuario tras el login" }
        return ResultadoSesionGoogle(firebaseUid = firebaseUser.uid, correo = firebaseUser.email)
    }
}
