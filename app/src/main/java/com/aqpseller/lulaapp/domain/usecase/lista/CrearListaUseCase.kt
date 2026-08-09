package com.aqpseller.lulaapp.domain.usecase.lista

import com.aqpseller.lulaapp.domain.repository.ListaRepository
import javax.inject.Inject

class CrearListaUseCase @Inject constructor(
    private val listaRepository: ListaRepository,
) {
    suspend operator fun invoke(espacioId: String, nombre: String, itemsTexto: List<String>, usuarioId: String): String =
        listaRepository.crear(espacioId, nombre, itemsTexto.filter { it.isNotBlank() }, usuarioId)
}
