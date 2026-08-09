package com.aqpseller.lulaapp.domain.usecase.diario

import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import javax.inject.Inject

class ActualizarEntradaDiarioUseCase @Inject constructor(
    private val entradaDiarioRepository: EntradaDiarioRepository,
) {
    suspend operator fun invoke(
        entradaId: String,
        usuarioId: String,
        titulo: String?,
        texto: String,
        areaDeVidaId: String?,
        fecha: Long,
    ) {
        val actual = entradaDiarioRepository.obtenerPorId(entradaId) ?: return
        entradaDiarioRepository.actualizar(
            actual.copy(
                titulo = titulo?.takeIf { it.isNotBlank() },
                texto = texto,
                areaDeVidaId = areaDeVidaId,
                fecha = fecha,
            ),
            usuarioId,
        )
    }
}
