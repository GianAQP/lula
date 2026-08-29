package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Un admin nombra a otro miembro como co-admin — puede haber varios admins a la vez, todos con
 * el mismo poder de agregar/quitar miembros. Solo el privilegio de eliminar el espacio completo
 * queda exclusivo para quien lo creó (ver `EliminarEspacioFamiliaUseCase`). A pedido del
 * usuario. Ver `Plan/08-decisiones-tecnicas.md`. */
class HacerAdminEspacioUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, miembroUsuarioId: String, miembroNombre: String?, miembroFirebaseUid: String?, ejecutadoPor: String) {
        val soyAdmin = espacioRepository.observarMiembros(espacioId).first()
            .find { it.usuarioId == ejecutadoPor }?.rol == RolEnEspacio.ADMIN
        if (!soyAdmin) return
        espacioRepository.agregarMiembro(espacioId, miembroUsuarioId, RolEnEspacio.ADMIN, miembroNombre, miembroFirebaseUid)
        if (miembroFirebaseUid != null) {
            runCatching { espacioSyncRepository.actualizarRolMiembro(espacioId, miembroFirebaseUid, RolEnEspacio.ADMIN) }
        }
    }
}
