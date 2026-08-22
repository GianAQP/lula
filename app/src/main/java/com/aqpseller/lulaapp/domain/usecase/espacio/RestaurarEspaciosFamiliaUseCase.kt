package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Restaura, en un celular nuevo, TODOS los Espacios Familia en los que la cuenta ya es miembro
 * (descubiertos vía `EspacioSyncRepository.descubrirMisEspacios`) — sin esto, la app no tiene
 * forma de saber que perteneces a "Familia X" hasta que alguien te vuelva a invitar. Trae
 * también el contenido (Tareas/Retos) de una sola vez; el sync en vivo de ahí en adelante lo
 * retoma `SincronizarEspacioFamiliaUseCase` cuando de verdad se entra a ese espacio. Pensado
 * para correr una sola vez (al vincular la cuenta, o en cada apertura de la app por si se unió
 * a algo nuevo desde otro dispositivo). Ver `Plan/12-firebase-auth-y-sync.md`.
 */
class RestaurarEspaciosFamiliaUseCase @Inject constructor(
    private val espacioSyncRepository: EspacioSyncRepository,
    private val espacioRepository: EspacioRepository,
    private val actividadRepository: ActividadRepository,
    private val retoFamiliarRepository: RetoFamiliarRepository,
) {
    suspend operator fun invoke(miUsuarioId: String) {
        val misEspacios = espacioSyncRepository.descubrirMisEspacios()
        misEspacios.forEach { (espacio, miembro) ->
            espacioRepository.asegurarEspacioMinimo(espacio.id, espacio.nombre, espacio.creadoPor, espacio.tipo)
            espacioRepository.agregarMiembro(espacio.id, miUsuarioId, miembro.rol)

            espacioSyncRepository.escucharTareas(espacio.id).first().forEach { (actividad, detalle) ->
                actividadRepository.mergeTareaRemota(actividad, detalle)
            }
            espacioSyncRepository.escucharRetos(espacio.id).first().forEach { reto ->
                retoFamiliarRepository.mergeRemoto(reto)
            }
            espacioSyncRepository.escucharRegistrosReto(espacio.id).first().forEach { registro ->
                retoFamiliarRepository.mergeRegistroRemoto(registro.retoId, registro.usuarioId, registro.fecha, registro.estado)
            }
        }
    }
}
