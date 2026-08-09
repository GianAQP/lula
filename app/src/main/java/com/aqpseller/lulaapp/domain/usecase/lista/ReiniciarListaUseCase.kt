package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

/** Desmarca todos los ítems para la próxima vez que se use la lista, sin tocar su plantilla. */
class ReiniciarListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(listaId: String, usuarioId: String) {
        listaRepository.reiniciar(listaId, usuarioId)
    }
}
