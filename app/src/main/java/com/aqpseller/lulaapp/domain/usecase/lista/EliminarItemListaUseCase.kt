package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

class EliminarItemListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(itemId: String, usuarioId: String) {
        listaRepository.eliminarItem(itemId, usuarioId)
    }
}
