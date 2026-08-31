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

    /**
     * Racha = total de días cerrados con ≥1 actividad cumplida, sin importar si hay huecos entre
     * medio — a propósito NO es "días consecutivos" (así era antes): si te saltas un día, la
     * racha simplemente se queda igual, nunca baja ni se rompe a 0. Un fuego que retrocede se
     * siente como un castigo por algo que ya pasó, no como algo que motive a seguir — a pedido
     * del usuario, y coincide con la práctica de apps de hábitos con "perdón" de días. Ver
     * `Plan/08-decisiones-tecnicas.md`.
     */
    suspend fun calcularRachaActual(espacioId: String): Int =
        registroDiarioRepository.observarHistorial(espacioId).first().count { it.actividadesCompletadas > 0 }

    /**
     * true si ayer se quedó sin cerrar y esta persona ya tiene el hábito de cerrar (al menos un
     * día cerrado antes) — así no le aparece a alguien que recién instaló la app y obviamente no
     * cerró "ayer". Pensado para un aviso motivador en Hoy que invite a cerrarlo desde el
     * calendario, no para culpar. Ver `Plan/08-decisiones-tecnicas.md`.
     */
    suspend fun diaAnteriorSinCerrar(espacioId: String): Boolean {
        val historial = registroDiarioRepository.observarHistorial(espacioId).first()
        if (historial.none { it.actividadesCompletadas > 0 }) return false
        val ayer = DateTimeUtils.hoy().toEpochDays().toLong() - 1
        return historial.none { it.fecha == ayer && it.actividadesCompletadas > 0 }
    }

    /**
     * Constancia = % de días activos (≥1 actividad cumplida) en los últimos 30 días —
     * independiente de la racha, no se resetea si se rompe (`01-arquitectura.md`).
     */
    suspend fun calcularConstancia(espacioId: String): Int {
        val hoy = DateTimeUtils.hoy().toEpochDays().toLong()
        val desde = hoy - 29
        val diasActivos = registroDiarioRepository.observarHistorial(espacioId).first()
            .count { it.fecha in desde..hoy && it.actividadesCompletadas > 0 }
        return diasActivos * 100 / 30
    }
}
