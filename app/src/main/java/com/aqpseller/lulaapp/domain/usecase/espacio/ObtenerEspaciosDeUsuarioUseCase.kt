package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerEspaciosDeUsuarioUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
) {
    operator fun invoke(usuarioId: String): Flow<List<Espacio>> = espacioRepository.observarEspaciosDeUsuario(usuarioId)
}
