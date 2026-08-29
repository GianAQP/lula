package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Baja a un co-admin a Miembro normal — contraparte de `HacerAdminEspacioUseCase`. Al creador
 * del espacio no lo puede bajar otro admin, mismo criterio que no lo pueden "Quitar" (ver
 * `EliminarMiembroEspacioUseCase`) — solo él mismo podría, si algún día se agrega esa opción. A
 * pedido del usuario. Ver `Plan/08-decisiones-tecnicas.md`. */
class QuitarAdminEspacioUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, miembroUsuarioId: String, miembroNombre: String?, miembroFirebaseUid: String?, ejecutadoPor: String) {
        val soyAdmin = espacioRepository.observarMiembros(espacioId).first()
            .find { it.usuarioId == ejecutadoPor }?.rol == RolEnEspacio.ADMIN
        if (!soyAdmin) return
        val espacio = espacioRepository.obtenerEspacioSiEsMiembro(espacioId, ejecutadoPor) ?: return
        if (miembroUsuarioId == espacio.creadoPor && ejecutadoPor != miembroUsuarioId) return
        espacioRepository.agregarMiembro(espacioId, miembroUsuarioId, RolEnEspacio.MIEMBRO, miembroNombre, miembroFirebaseUid)
        if (miembroFirebaseUid != null) {
            runCatching { espacioSyncRepository.actualizarRolMiembro(espacioId, miembroFirebaseUid, RolEnEspacio.MIEMBRO) }
        }
    }
}
