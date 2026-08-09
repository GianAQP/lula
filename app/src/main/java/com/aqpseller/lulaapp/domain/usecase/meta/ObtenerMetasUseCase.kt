package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerMetasUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<Meta>> = metaRepository.observarPorEspacio(espacioId)
}
