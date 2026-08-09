package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

class EliminarListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(listaId: String, usuarioId: String) {
        listaRepository.eliminarLista(listaId, usuarioId)
    }
}
