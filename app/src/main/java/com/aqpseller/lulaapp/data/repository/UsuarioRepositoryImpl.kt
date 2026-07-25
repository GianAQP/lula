package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.data.local.dao.UsuarioDao
import com.aqpseller.lulaapp.data.local.entity.UsuarioEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.Usuario
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val auditLogger: AuditLogger,
) : UsuarioRepository {

    override fun observarUsuario(): Flow<Usuario?> =
        usuarioDao.observarUnico().map { it?.toDomain() }

    override suspend fun contarUsuarios(): Int = usuarioDao.contar()

    override suspend fun crearUsuario(usuario: Usuario) {
        val entity = usuario.toEntity()
        usuarioDao.upsert(entity)
        auditLogger.registrar<UsuarioEntity>(
            entidad = "usuario",
            entidadId = usuario.id,
            accion = AccionAuditoria.CREAR,
            despues = entity,
            usuarioId = usuario.id,
        )
    }
}
