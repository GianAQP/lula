package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import javax.inject.Inject

/**
 * Cambia solo la fecha límite de una meta — el atajo rápido "🔜 Aplazar" desde el detalle, sin
 * pasar por el formulario completo de editar. Reprograma el aviso sonoro si la meta lo tiene
 * activado. Ver `Plan/08-decisiones-tecnicas.md`.
 */
class AplazarMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
) {
    suspend operator fun invoke(metaId: String, nuevaFechaLimite: Long, usuarioId: String) {
        val meta = metaRepository.obtenerConVinculo(metaId) ?: return
        metaRepository.aplazarFechaLimite(metaId, nuevaFechaLimite, usuarioId)
        recordatorioScheduler.cancelarMeta(metaId)
        if (meta.avisarAlVencer) {
            recordatorioScheduler.programarMeta(metaId, meta.nombre, nuevaFechaLimite)
        }
    }
}
