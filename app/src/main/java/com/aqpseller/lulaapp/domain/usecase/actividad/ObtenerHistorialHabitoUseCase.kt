package com.aqpseller.lulaapp.domain.usecase.actividad

import com.aqpseller.lulaapp.domain.model.DiaHistorialHabito
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import javax.inject.Inject

class ObtenerHistorialHabitoUseCase @Inject constructor(
    private val actividadRepository: ActividadRepository,
) {
    suspend fun ultimosDias(actividadId: String, dias: Int): List<DiaHistorialHabito> =
        actividadRepository.obtenerHistorialHabito(actividadId, dias)

    /** Racha del hábito = días consecutivos confirmados contando hacia atrás desde hoy. */
    suspend fun calcularRacha(actividadId: String): Int {
        val historial = actividadRepository.obtenerHistorialHabito(actividadId, 60)
        var racha = 0
        for (dia in historial.asReversed()) {
            if (dia.estado != EstadoActividad.CONFIRMADO) break
            racha++
        }
        return racha
    }
}
