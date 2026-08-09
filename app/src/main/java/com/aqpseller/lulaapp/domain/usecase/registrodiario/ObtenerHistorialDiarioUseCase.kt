package com.aqpseller.lulaapp.domain.usecase.registrodiario

import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerHistorialDiarioUseCase @Inject constructor(
    private val registroDiarioRepository: RegistroDiarioRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<RegistroDiario>> =
        registroDiarioRepository.observarHistorial(espacioId)
}
