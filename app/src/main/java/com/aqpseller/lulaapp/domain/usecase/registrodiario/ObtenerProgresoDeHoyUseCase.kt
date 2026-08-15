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
     * Racha = días consecutivos con el día cerrado y ≥1 actividad cumplida (`01-arquitectura.md`).
     * Si hoy todavía no se cerró, no cuenta como "racha rota" — se sigue mostrando la racha hasta
     * ayer (cerrar hoy la extiende en +1, no aparece de golpe). Antes se empezaba a contar
     * siempre desde hoy, así que una racha real de varios días se veía en 0 toda la mañana hasta
     * cerrar el día, como si se hubiera perdido sin haberse perdido — a pedido del usuario. Ver
     * `Plan/08-decisiones-tecnicas.md`.
     */
    suspend fun calcularRachaActual(espacioId: String): Int {
        val historialPorFecha = registroDiarioRepository.observarHistorial(espacioId).first()
            .associateBy { it.fecha }
        var fecha = DateTimeUtils.hoy().toEpochDays().toLong()
        if ((historialPorFecha[fecha]?.actividadesCompletadas ?: 0) <= 0) fecha--
        var racha = 0
        while (true) {
            val registro = historialPorFecha[fecha] ?: break
            if (registro.actividadesCompletadas <= 0) break
            racha++
            fecha--
        }
        return racha
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
