package com.aqpseller.lulaapp.domain.usecase.diario

import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class ActualizarEntradaDiarioUseCase @Inject constructor(
    private val entradaDiarioRepository: EntradaDiarioRepository,
    private val personalSyncRepository: PersonalSyncRepository,
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
        val actualizada = actual.copy(
            titulo = titulo?.takeIf { it.isNotBlank() },
            texto = texto,
            areaDeVidaId = areaDeVidaId,
            fecha = fecha,
        )
        entradaDiarioRepository.actualizar(actualizada, usuarioId)
        runCatching { personalSyncRepository.subirEntradaDiario(actualizada) }
    }
}
