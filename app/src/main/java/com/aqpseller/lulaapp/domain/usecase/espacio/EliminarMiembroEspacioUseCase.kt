package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Un admin quita a otra persona de un Espacio Familia — el contenido que esa persona ya había
 * creado se queda tal cual, solo se corta su acceso. No-op silencioso si [ejecutadoPor] no es
 * admin (la regla de seguridad de Firestore también lo exige del lado del servidor). Al
 * creador del espacio no lo puede sacar otro admin — solo puede salir él mismo (mismo criterio
 * que `SalirDeEspacioFamiliaUseCase`), hasta que decida dejarlo. Puede haber varios admins
 * (co-admins, ver `HacerAdminEspacioUseCase`) pero solo el creador tiene ese privilegio
 * especial. A pedido del usuario. Ver `Plan/08-decisiones-tecnicas.md`. */
class EliminarMiembroEspacioUseCase @Inject constructor(
    private val espacioRepository: EspacioRepository,
    private val espacioSyncRepository: EspacioSyncRepository,
) {
    suspend operator fun invoke(espacioId: String, miembroUsuarioId: String, miembroFirebaseUid: String?, ejecutadoPor: String) {
        val soyAdmin = espacioRepository.observarMiembros(espacioId).first()
            .find { it.usuarioId == ejecutadoPor }?.rol == RolEnEspacio.ADMIN
        if (!soyAdmin) return
        val espacio = espacioRepository.obtenerEspacioSiEsMiembro(espacioId, ejecutadoPor) ?: return
        if (miembroUsuarioId == espacio.creadoPor && ejecutadoPor != miembroUsuarioId) return
        espacioRepository.eliminarMiembro(espacioId, miembroUsuarioId, ejecutadoPor)
        if (miembroFirebaseUid != null) {
            runCatching { espacioSyncRepository.eliminarMiembro(espacioId, miembroFirebaseUid) }
        }
    }
}
