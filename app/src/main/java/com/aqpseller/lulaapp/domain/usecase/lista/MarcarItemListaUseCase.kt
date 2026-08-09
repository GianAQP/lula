package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

class MarcarItemListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(itemId: String, marcado: Boolean, usuarioId: String) {
        listaRepository.marcarItem(itemId, marcado, usuarioId)
    }
}
