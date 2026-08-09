package com.aqpseller.lulaapp.domain.usecase.meta

import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerHistorialHabitoUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val HITOS = listOf(100, 75, 50, 25)

/** Meta con su progreso ya resuelto — para `POR_HABITO`, cuenta en vivo contra el historial del
 * hábito vinculado en vez de guardarlo (`Meta.valorActual` se ignora en ese caso). */
data class MetaConProgreso(val meta: Meta, val progreso: Double) {
    val fraccion: Float
        get() = if (meta.valorObjetivo > 0) (progreso / meta.valorObjetivo).toFloat().coerceIn(0f, 1f) else 0f

    /** Último hito (100/75/50/25) que el progreso actual ya cruzó, 0 si ninguno. */
    val hitoActual: Int
        get() = HITOS.firstOrNull { fraccion * 100 >= it } ?: 0

    /** Hay algo nuevo que celebrar — cruzó un hito que todavía no se le mostró. */
    val hayHitoNuevo: Boolean
        get() = hitoActual > meta.ultimoHitoCelebrado

    /** Null si no tiene fecha límite o ya se completó — no tiene sentido apurar algo ya logrado. */
    val diasRestantes: Int?
        get() {
            if (meta.fechaLimite == null || hitoActual >= 100) return null
            val hoy = DateTimeUtils.hoy()
            val limite = DateTimeUtils.epochMillisToLocalDate(meta.fechaLimite)
            return limite.toEpochDays() - hoy.toEpochDays()
        }

    /** Últimos 7 días antes de la fecha límite (o ya vencida) — para resaltarla en Hoy sin
     * convertir la fecha límite en presión constante desde el día 1. */
    val esUrgente: Boolean
        get() = diasRestantes?.let { it <= 7 } ?: false
}

/**
 * Único lugar que calcula el progreso de una Meta — antes esta cuenta vivía duplicada en
 * `GoalsListViewModel`, y el riesgo (mismo que causó el bug de "9 de 14" en Cerrar día) es que
 * otra pantalla la reimplemente distinto y los números no coincidan. Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
class ObtenerMetasConProgresoUseCase @Inject constructor(
    private val obtenerMetasUseCase: ObtenerMetasUseCase,
    private val obtenerHistorialHabitoUseCase: ObtenerHistorialHabitoUseCase,
) {
    operator fun invoke(espacioId: String): Flow<List<MetaConProgreso>> =
        obtenerMetasUseCase(espacioId).map { metas -> metas.map { it.conProgreso() } }

    private suspend fun Meta.conProgreso(): MetaConProgreso {
        val habitoId = actividadesVinculadasIds.firstOrNull()
        val esPorHabito = comoSeMide == ComoSeMideMeta.POR_HABITO && habitoId != null
        val progreso = if (esPorHabito) {
            val dias = valorObjetivo.toInt().coerceAtLeast(1)
            obtenerHistorialHabitoUseCase.ultimosDias(habitoId!!, dias).count { it.estado == EstadoActividad.CONFIRMADO }.toDouble()
        } else {
            valorActual
        }
        return MetaConProgreso(this, progreso)
    }
}
