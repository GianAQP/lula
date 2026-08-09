package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.model.ListaEjecucion
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerHistorialListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    operator fun invoke(listaId: String): Flow<List<ListaEjecucion>> = listaRepository.observarHistorial(listaId)
}
