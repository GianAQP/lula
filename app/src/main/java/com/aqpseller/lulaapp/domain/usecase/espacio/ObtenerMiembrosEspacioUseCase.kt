package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerMiembrosEspacioUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<EspacioMiembro>> = espacioRepository.observarMiembros(espacioId)
}
