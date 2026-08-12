package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

class ActualizarOrdenListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(listaId: String, nuevoOrden: Int, usuarioId: String) =
        listaRepository.actualizarOrdenLista(listaId, nuevoOrden, usuarioId)
}
