package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

class AgregarItemListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(listaId: String, texto: String, usuarioId: String) {
        if (texto.isBlank()) return
        listaRepository.agregarItem(listaId, texto, usuarioId)
    }
}
