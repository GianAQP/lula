package com.aqpseller.lulaapp.domain.usecase.diario

import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import javax.inject.Inject

class ObtenerEntradaDiarioUseCase @Inject constructor(
    private val entradaDiarioRepository: EntradaDiarioRepository,
) {
    suspend operator fun invoke(entradaId: String): EntradaDiario? = entradaDiarioRepository.obtenerPorId(entradaId)
}
