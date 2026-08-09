package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import javax.inject.Inject

class ActualizarMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
) {
    suspend operator fun invoke(
        metaId: String,
        espacioId: String,
        usuarioId: String,
        nombre: String,
        comoSeMide: ComoSeMideMeta,
        valorObjetivo: Double,
        valorActual: Double,
        actividadVinculadaId: String? = null,
        areaDeVidaId: String? = null,
        fechaLimite: Long? = null,
    ) {
        // Se preserva el hito ya celebrado — si no, cualquier edición (aunque sea solo el
        // nombre) volvería a mostrar "¡llegaste al 50%!" de la nada. Ver `08-decisiones-tecnicas.md`.
        val hitoActual = metaRepository.obtenerConVinculo(metaId)?.ultimoHitoCelebrado ?: 0
        val meta = Meta(
            id = metaId,
            espacioId = espacioId,
            nombre = nombre,
            areaDeVidaId = areaDeVidaId,
            fechaLimite = fechaLimite,
            comoSeMide = comoSeMide,
            valorObjetivo = valorObjetivo,
            valorActual = valorActual,
            actividadesVinculadasIds = listOfNotNull(actividadVinculadaId),
            ultimoHitoCelebrado = hitoActual,
        )
        metaRepository.actualizar(meta, usuarioId)
    }
}
