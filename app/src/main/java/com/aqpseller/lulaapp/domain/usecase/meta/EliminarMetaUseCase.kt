package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.domain.repository.MetaRepository
import javax.inject.Inject

class EliminarMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
) {
    suspend operator fun invoke(metaId: String, usuarioId: String) {
        metaRepository.eliminar(metaId, usuarioId)
    }
}
