package com.aqpseller.lulaapp.domain.usecase.registrodiario

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.IdGenerator
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import javax.inject.Inject

class CerrarDiaUseCase @Inject constructor(
    private val registroDiarioRepository: RegistroDiarioRepository,
) {
    suspend operator fun invoke(
        espacioId: String,
        usuarioId: String,
        actividadesCompletadas: Int,
        actividadesTotales: Int,
        queLogre: String?,
        queCosto: String?,
        queAjusto: String?,
        fechaEpochDay: Long = DateTimeUtils.hoy().toEpochDays().toLong(),
    ): RegistroDiario {
        val existente = registroDiarioRepository.obtenerPorFecha(espacioId, fechaEpochDay)
        val registro = RegistroDiario(
            id = existente?.id ?: IdGenerator.newId(),
            espacioId = espacioId,
            fecha = fechaEpochDay,
            actividadesCompletadas = actividadesCompletadas,
            actividadesTotales = actividadesTotales,
            // 1 punto por actividad cumplida, sin techo — decisión de MVP en `01-arquitectura.md`.
            puntuacion = actividadesCompletadas,
            queLogre = queLogre,
            queCosto = queCosto,
            queAjusto = queAjusto,
        )
        registroDiarioRepository.cerrarDia(registro, usuarioId)
        return registro
    }
}
