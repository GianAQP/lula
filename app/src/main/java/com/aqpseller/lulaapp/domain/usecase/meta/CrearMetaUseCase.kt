package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.core.notifications.RecordatorioScheduler
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.CategoriaMeta
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.model.NivelRecordatorio
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class CrearMetaUseCase @Inject constructor(
    private val metaRepository: MetaRepository,
    private val recordatorioScheduler: RecordatorioScheduler,
    private val personalSyncRepository: PersonalSyncRepository,
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
        categoria: CategoriaMeta? = null,
        nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
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
            categoria = categoria,
            nivelRecordatorio = nivelRecordatorio,
        )
        metaRepository.crear(meta, usuarioId)
        runCatching { personalSyncRepository.subirMeta(meta) }
        if (fechaLimite != null) {
            recordatorioScheduler.programarMeta(meta.id, nombre, fechaLimite, nivelRecordatorio)
        }
    }
}
