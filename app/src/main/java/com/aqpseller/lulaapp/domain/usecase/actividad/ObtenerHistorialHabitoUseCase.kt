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

    /**
     * Racha del hábito = días consecutivos confirmados contando hacia atrás desde hoy. Si hoy
     * todavía no se marcó, no cuenta como "racha rota" — arranca desde ayer, mismo criterio que
     * la racha global (`ObtenerProgresoDeHoyUseCase`), para no mostrar 0 toda la mañana en un
     * hábito con racha real en curso. Ver `Plan/08-decisiones-tecnicas.md`.
     */
    suspend fun calcularRacha(actividadId: String): Int {
        val historial = actividadRepository.obtenerHistorialHabito(actividadId, 60)
        var dias = historial.asReversed()
        if (dias.firstOrNull()?.estado != EstadoActividad.CONFIRMADO) dias = dias.drop(1)
        var racha = 0
        for (dia in dias) {
            if (dia.estado != EstadoActividad.CONFIRMADO) break
            racha++
        }
        return racha
    }
}
