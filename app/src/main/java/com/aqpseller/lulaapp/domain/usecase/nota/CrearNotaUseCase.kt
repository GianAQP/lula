package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class CrearNotaUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, propietario: String, titulo: String?, contenido: String) {
        val ahora = DateTimeUtils.ahoraEpochMillis()
        val nota = Nota(
            id = IdGenerator.newId(),
            espacioId = espacioId,
            propietario = propietario,
            titulo = titulo?.takeIf { it.isNotBlank() },
            contenido = contenido,
            fechaCreacion = ahora,
            fechaEdicion = ahora,
            orden = notaRepository.obtenerOrdenParaNota(espacioId),
        )
        notaRepository.crear(nota)
        runCatching { personalSyncRepository.subirNota(nota) }
    }
}
