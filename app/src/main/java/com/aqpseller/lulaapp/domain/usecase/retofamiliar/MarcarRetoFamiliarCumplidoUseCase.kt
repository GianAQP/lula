package com.aqpseller.lulaapp.domain.usecase.retofamiliar

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.RegistroRetoRemoto
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import javax.inject.Inject

class MarcarRetoFamiliarCumplidoUseCase @Inject constructor(
    private val retoFamiliarRepository: RetoFamiliarRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, retoId: String, usuarioId: String, cumplido: Boolean) {
        retoFamiliarRepository.marcarCumplidoHoy(retoId, usuarioId, cumplido)
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val estado = if (cumplido) EstadoActividad.CONFIRMADO else EstadoActividad.SIN_CONFIRMAR
        runCatching {
            espacioSyncRepository.subirRegistroReto(espacioId, RegistroRetoRemoto(retoId, usuarioId, fechaHoy, estado))
        }
    }
}
