package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class ActualizarNotaUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(notaId: String, usuarioId: String, titulo: String?, contenido: String) {
        val actual = notaRepository.obtenerPorId(notaId) ?: return
        val actualizada = actual.copy(titulo = titulo?.takeIf { it.isNotBlank() }, contenido = contenido, fechaEdicion = DateTimeUtils.ahoraEpochMillis())
        notaRepository.actualizar(actualizada, usuarioId)
        runCatching { personalSyncRepository.subirNota(actualizada) }
    }
}
