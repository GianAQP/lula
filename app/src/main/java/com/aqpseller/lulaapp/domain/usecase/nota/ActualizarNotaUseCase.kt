package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import javax.inject.Inject

class ActualizarNotaUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
) {
    suspend operator fun invoke(notaId: String, usuarioId: String, titulo: String?, contenido: String) {
        val actual = notaRepository.obtenerPorId(notaId) ?: return
        notaRepository.actualizar(
            actual.copy(titulo = titulo?.takeIf { it.isNotBlank() }, contenido = contenido, fechaEdicion = DateTimeUtils.ahoraEpochMillis()),
            usuarioId,
        )
    }
}
