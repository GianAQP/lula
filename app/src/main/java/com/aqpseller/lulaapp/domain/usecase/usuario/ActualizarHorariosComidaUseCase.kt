package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import javax.inject.Inject

class ActualizarHorariosComidaUseCase @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(usuarioId: String, horaDesayuno: String?, horaAlmuerzo: String?, horaCena: String?) {
        usuarioRepository.actualizarHorariosComida(usuarioId, horaDesayuno, horaAlmuerzo, horaCena)
    }
}
