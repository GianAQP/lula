package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.database.LulaDatabase
import com.aqpseller.lulaapp.data.local.dao.UsuarioDao
import com.aqpseller.lulaapp.data.local.entity.UsuarioEntity
import com.aqpseller.lulaapp.domain.model.AccionAuditoria
import com.aqpseller.lulaapp.domain.model.MetodoLogin
import com.aqpseller.lulaapp.domain.model.Usuario
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val auditLogger: AuditLogger,
    private val lulaDatabase: LulaDatabase,
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

    override suspend fun actualizarHorariosComida(usuarioId: String, horaDesayuno: String?, horaAlmuerzo: String?, horaCena: String?) {
        val actual = usuarioDao.obtenerUnico() ?: return
        val actualizado = actual.copy(horaDesayuno = horaDesayuno, horaAlmuerzo = horaAlmuerzo, horaCena = horaCena)
        usuarioDao.upsert(actualizado)
        auditLogger.registrar<UsuarioEntity>(
            entidad = "usuario",
            entidadId = usuarioId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    override suspend fun actualizarConsentimientos(
        usuarioId: String,
        confirmoMayorDe13: Boolean?,
        terminosAceptadosEn: Long?,
        consentimientoDatosSaludEn: Long?,
    ) {
        val actual = usuarioDao.obtenerUnico() ?: return
        val actualizado = actual.copy(
            confirmoMayorDe13 = confirmoMayorDe13 ?: actual.confirmoMayorDe13,
            terminosAceptadosEn = terminosAceptadosEn ?: actual.terminosAceptadosEn,
            consentimientoDatosSaludEn = consentimientoDatosSaludEn ?: actual.consentimientoDatosSaludEn,
        )
        usuarioDao.upsert(actualizado)
        auditLogger.registrar<UsuarioEntity>(
            entidad = "usuario",
            entidadId = usuarioId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }

    /** Sin auditar — la propia tabla de auditoría queda vacía después de esto, no tiene sentido
     * dejar una fila de "se borró todo" en un historial que ya no existe. */
    override suspend fun eliminarCuenta() {
        withContext(Dispatchers.IO) {
            lulaDatabase.clearAllTables()
        }
    }

    override suspend fun vincularConGoogle(usuarioId: String, correo: String, firebaseUid: String) {
        val actual = usuarioDao.obtenerUnico() ?: return
        val actualizado = actual.copy(
            correo = correo,
            metodoLogin = MetodoLogin.GOOGLE.name,
            firebaseUid = firebaseUid,
        )
        usuarioDao.upsert(actualizado)
        auditLogger.registrar<UsuarioEntity>(
            entidad = "usuario",
            entidadId = usuarioId,
            accion = AccionAuditoria.ACTUALIZAR,
            antes = actual,
            despues = actualizado,
            usuarioId = usuarioId,
        )
    }
}
