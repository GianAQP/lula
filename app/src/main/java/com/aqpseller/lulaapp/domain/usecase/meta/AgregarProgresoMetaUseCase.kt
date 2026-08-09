package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.domain.repository.MetaRepository
import javax.inject.Inject

class AgregarProgresoMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
) {
    suspend operator fun invoke(metaId: String, incremento: Double, usuarioId: String) {
        metaRepository.agregarProgreso(metaId, incremento, usuarioId)
    }
}
