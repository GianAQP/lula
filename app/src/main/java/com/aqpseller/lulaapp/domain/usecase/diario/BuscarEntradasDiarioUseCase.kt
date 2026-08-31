package com.aqpseller.lulaapp.domain.usecase.diario

import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BuscarEntradasDiarioUseCase @Inject constructor(
    private val entradaDiarioRepository: EntradaDiarioRepository,
) {
    operator fun invoke(espacioId: String, consulta: String): Flow<List<EntradaDiario>> =
        entradaDiarioRepository.buscar(espacioId, consulta)
}
