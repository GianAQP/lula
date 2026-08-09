package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import javax.inject.Inject

class ObtenerDetalleMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
) {
    suspend operator fun invoke(metaId: String): Meta? = metaRepository.obtenerConVinculo(metaId)
}
