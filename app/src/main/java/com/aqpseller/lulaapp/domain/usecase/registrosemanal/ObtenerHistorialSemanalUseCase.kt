package com.aqpseller.lulaapp.domain.usecase.registrosemanal

import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.repository.RegistroSemanalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerHistorialSemanalUseCase @Inject constructor(
    private val registroSemanalRepository: RegistroSemanalRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<RegistroSemanal>> = registroSemanalRepository.observarHistorial(espacioId)
}
