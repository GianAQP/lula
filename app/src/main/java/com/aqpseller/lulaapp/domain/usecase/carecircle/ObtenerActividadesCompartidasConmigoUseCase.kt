package com.aqpseller.lulaapp.domain.usecase.carecircle

import com.aqpseller.lulaapp.domain.repository.ActividadCompartidaRemota
import com.aqpseller.lulaapp.domain.repository.CareCircleContenidoSyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Todo lo que otras personas me comparten a mí, en vivo — usado por "Lo que me comparten". */
class ObtenerActividadesCompartidasConmigoUseCase @Inject constructor(
    private val careCircleContenidoSyncRepository: CareCircleContenidoSyncRepository,
) {
    operator fun invoke(miCorreo: String): Flow<List<ActividadCompartidaRemota>> =
        careCircleContenidoSyncRepository.escucharActividadesCompartidasConmigo(miCorreo)
}
