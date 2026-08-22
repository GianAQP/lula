package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EliminarItemListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(itemId: String, usuarioId: String) {
        val listaId = listaRepository.obtenerListaIdDeItem(itemId)
        listaRepository.eliminarItem(itemId, usuarioId)
        if (listaId != null) {
            listaRepository.observarConItems(listaId).first()?.let { lista ->
                runCatching { personalSyncRepository.subirLista(lista) }
            }
        }
    }
}
