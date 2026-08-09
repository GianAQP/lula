package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.model.ListaResumen
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerListasUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<ListaResumen>> = listaRepository.observarResumenPorEspacio(espacioId)
}
