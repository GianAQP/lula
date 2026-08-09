package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import javax.inject.Inject

class CrearMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        usuarioId: String,
        nombre: String,
        comoSeMide: ComoSeMideMeta,
        valorObjetivo: Double,
        actividadVinculadaId: String? = null,
        areaDeVidaId: String? = null,
        fechaLimite: Long? = null,
    ) {
        val meta = Meta(
            id = IdGenerator.newId(),
            espacioId = espacioId,
            nombre = nombre,
            areaDeVidaId = areaDeVidaId,
            fechaLimite = fechaLimite,
            comoSeMide = comoSeMide,
            valorObjetivo = valorObjetivo,
            valorActual = 0.0,
            actividadesVinculadasIds = listOfNotNull(actividadVinculadaId),
        )
        metaRepository.crear(meta, usuarioId)
    }
}
