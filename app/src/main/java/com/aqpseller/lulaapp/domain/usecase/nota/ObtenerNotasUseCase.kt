package com.aqpseller.lulaapp.domain.usecase.nota

import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerNotasUseCase @Inject constructor(
    private val notaRepository: NotaRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<Nota>> = notaRepository.observarPorEspacio(espacioId)
}
