package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun observarUsuario(): Flow<Usuario?>
    suspend fun contarUsuarios(): Int
    suspend fun crearUsuario(usuario: Usuario)
}
