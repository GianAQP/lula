package com.aqpseller.lulaapp.domain.usecase.registrodiario

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ObtenerProgresoDeHoyUseCase @Inject constructor(
    private val registroDiarioRepository: RegistroDiarioRepository,
) {
    suspend fun registroDeHoy(espacioId: String): RegistroDiario? {
        val fechaHoy = DateTimeUtils.hoy().toEpochDays().toLong()
        return registroDiarioRepository.obtenerPorFecha(espacioId, fechaHoy)
    }

    /** Racha = días consecutivos con el día cerrado y ≥1 actividad cumplida (`01-arquitectura.md`). */
    suspend fun calcularRachaActual(espacioId: String): Int {
        val historialPorFecha = registroDiarioRepository.observarHistorial(espacioId).first()
            .associateBy { it.fecha }
        var racha = 0
        var fecha = DateTimeUtils.hoy().toEpochDays().toLong()
        while (true) {
            val registro = historialPorFecha[fecha] ?: break
            if (registro.actividadesCompletadas <= 0) break
            racha++
            fecha--
        }
        return racha
    }
}
