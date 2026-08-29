package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import javax.inject.Inject

/** Borra el espacio Familia y todo lo que tenía dentro — para siempre. Si era el espacio
 * activo, vuelve a Personal de una (nunca se queda apuntando a un espacio que ya no existe).
 * Solo tiene efecto si [usuarioId] es quien CREÓ el espacio (no-op silencioso si no) — a
 * diferencia de quitar miembros, que cualquier admin puede hacer, borrar el grupo entero es un
 * privilegio exclusivo del creador; un co-admin (ver `HacerAdminEspacioUseCase`) no puede. A
 * pedido del usuario. También borra el espacio de Firestore — sin esto, la próxima
 * apertura de la app lo traía de vuelta vía `RestaurarEspaciosFamiliaUseCase` (el documento y la
 * membresía seguían existiendo en la nube). Ver `Plan/08-decisiones-tecnicas.md`. */
class EliminarEspacioFamiliaUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
    private val ajustesRepository: AjustesRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, usuarioId: String) {
        val espacio = espacioRepository.obtenerEspacioSiEsMiembro(espacioId, usuarioId) ?: return
        if (espacio.creadoPor != usuarioId) return
        espacioRepository.eliminarEspacio(espacioId, usuarioId)
        if (ajustesRepository.obtenerEspacioActivoId() == espacioId) {
            ajustesRepository.setEspacioActivoId(null)
        }
        runCatching { espacioSyncRepository.eliminarEspacioCompleto(espacioId) }
    }
}
