package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CrearListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
    private val personalSyncRepository: PersonalSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, nombre: String, itemsTexto: List<String>, usuarioId: String): String {
        val listaId = listaRepository.crear(espacioId, nombre, itemsTexto.filter { it.isNotBlank() }, usuarioId)
        listaRepository.observarConItems(listaId).first()?.let { lista ->
            runCatching { personalSyncRepository.subirLista(lista) }
        }
        return listaId
    }
}
