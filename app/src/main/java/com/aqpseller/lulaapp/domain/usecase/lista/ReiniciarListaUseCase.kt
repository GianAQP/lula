package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Desmarca todos los ítems para la próxima vez que se use la lista, sin tocar su plantilla. */
class ReiniciarListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(listaId: String, usuarioId: String) {
        listaRepository.reiniciar(listaId, usuarioId)
        listaRepository.observarConItems(listaId).first()?.let { lista ->
            runCatching { personalSyncRepository.subirLista(lista) }
        }
    }
}
