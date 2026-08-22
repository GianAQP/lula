package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class EliminarMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(metaId: String, usuarioId: String) {
        metaRepository.eliminar(metaId, usuarioId)
        runCatching { personalSyncRepository.eliminarMeta(metaId) }
        recordatorioScheduler.cancelarMeta(metaId)
    }
}
