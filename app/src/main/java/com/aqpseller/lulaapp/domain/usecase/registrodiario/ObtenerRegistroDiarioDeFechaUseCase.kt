package com.aqpseller.lulaapp.domain.usecase.registrodiario

import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import javax.inject.Inject

/** Usado por el Calendario (vista Día) para mostrar el cierre de ese día, si existe. */
class ObtenerRegistroDiarioDeFechaUseCase @Inject constructor(
    private val registroDiarioRepository: RegistroDiarioRepository,
) {
    suspend operator fun invoke(espacioId: String, fecha: Long): RegistroDiario? =
        registroDiarioRepository.obtenerPorFecha(espacioId, fecha)
}
