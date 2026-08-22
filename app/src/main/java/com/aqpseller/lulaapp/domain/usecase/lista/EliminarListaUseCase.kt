package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

class EliminarListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(listaId: String, usuarioId: String) {
        listaRepository.eliminarLista(listaId, usuarioId)
        runCatching { personalSyncRepository.eliminarLista(listaId) }
    }
}
