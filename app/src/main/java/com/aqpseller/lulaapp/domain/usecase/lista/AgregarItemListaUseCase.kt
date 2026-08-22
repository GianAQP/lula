package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AgregarItemListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(listaId: String, texto: String, usuarioId: String) {
        if (texto.isBlank()) return
        listaRepository.agregarItem(listaId, texto, usuarioId)
        listaRepository.observarConItems(listaId).first()?.let { lista ->
            runCatching { personalSyncRepository.subirLista(lista) }
        }
    }
}
