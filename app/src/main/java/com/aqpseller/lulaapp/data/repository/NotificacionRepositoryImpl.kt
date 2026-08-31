package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.data.local.dao.NotificacionDao
import com.aqpseller.lulaapp.data.local.entity.NotificacionEntity
import com.aqpseller.lulaapp.domain.model.Notificacion
import com.aqpseller.lulaapp.domain.repository.NotificacionRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificacionRepositoryImpl @Inject constructor(
    private val notificacionDao: NotificacionDao,
    private val personalSyncRepository: PersonalSyncRepository,
) : NotificacionRepository {

    override suspend fun registrar(emoji: String, titulo: String, cuerpo: String, solicitudId: String?) {
        val entity = NotificacionEntity(
            id = IdGenerator.newId(),
            emoji = emoji,
            titulo = titulo,
            cuerpo = cuerpo,
            fecha = DateTimeUtils.ahoraEpochMillis(),
            leido = false,
            solicitudId = solicitudId,
        )
        notificacionDao.insertar(entity)
        runCatching { personalSyncRepository.subirNotificacion(entity.toDomain()) }
    }

    override fun observarTodas(): Flow<List<Notificacion>> =
        notificacionDao.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override fun observarNoLeidas(): Flow<Int> = notificacionDao.observarNoLeidas()

    override suspend fun marcarLeida(id: String) {
        notificacionDao.marcarLeida(id)
        runCatching { personalSyncRepository.marcarNotificacionLeidaRemota(id) }
    }

    override suspend fun restaurarDesdeRemota(notificacion: Notificacion) {
        notificacionDao.upsert(notificacion.toEntity())
    }
}

private fun Notificacion.toEntity() = NotificacionEntity(
    id = id,
    emoji = emoji,
    titulo = titulo,
    cuerpo = cuerpo,
    fecha = fecha,
    leido = leido,
    solicitudId = solicitudId,
)

private fun NotificacionEntity.toDomain() = Notificacion(
    id = id,
    emoji = emoji,
    titulo = titulo,
    cuerpo = cuerpo,
    fecha = fecha,
    leido = leido,
    solicitudId = solicitudId,
)
