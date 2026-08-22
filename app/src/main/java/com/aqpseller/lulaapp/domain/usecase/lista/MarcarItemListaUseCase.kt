package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MarcarItemListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(itemId: String, marcado: Boolean, usuarioId: String) {
        listaRepository.marcarItem(itemId, marcado, usuarioId)
        val listaId = listaRepository.obtenerListaIdDeItem(itemId) ?: return
        listaRepository.observarConItems(listaId).first()?.let { lista ->
            runCatching { personalSyncRepository.subirLista(lista) }
        }
    }
}
