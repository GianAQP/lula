package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.EventoEliminacionMiembro
import com.aqpseller.lulaapp.domain.repository.HistorialCambiosRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Historial de "quitar de un Espacio" (admin quita a alguien, o alguien sale solo) — a pedido
 * del usuario, visible solo para admins (la pantalla decide eso, este caso de uso no filtra por
 * quién lo pide). Ver `Plan/08-decisiones-tecnicas.md`. */
class ObtenerHistorialMiembrosEspacioUseCase @Inject constructor(
    private val historialCambiosRepository: HistorialCambiosRepository,
) {
    operator fun invoke(espacioId: String): Flow<List<EventoEliminacionMiembro>> =
        historialCambiosRepository.observarEliminacionesDeMiembros(espacioId)
}
