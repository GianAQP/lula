package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.model.ListaConItems
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerListaConItemsUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    operator fun invoke(listaId: String): Flow<ListaConItems?> = listaRepository.observarConItems(listaId)
}
