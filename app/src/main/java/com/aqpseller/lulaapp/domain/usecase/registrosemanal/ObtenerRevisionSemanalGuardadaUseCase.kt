package com.aqpseller.lulaapp.domain.usecase.registrosemanal

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.repository.RegistroSemanalRepository
import javax.inject.Inject

class ObtenerRevisionSemanalGuardadaUseCase @Inject constructor(
    private val registroSemanalRepository: RegistroSemanalRepository,
) {
    suspend operator fun invoke(espacioId: String): RegistroSemanal? =
        registroSemanalRepository.obtenerPorSemana(espacioId, DateTimeUtils.claveSemana())
}
