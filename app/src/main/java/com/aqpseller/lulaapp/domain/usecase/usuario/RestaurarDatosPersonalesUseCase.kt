package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import javax.inject.Inject

/**
 * Restaura el respaldo de Hábitos/Tareas personales desde Firestore — pensado para correr UNA
 * VEZ (al vincular la cuenta con Google, o al abrir la app si ya estaba vinculada), no un
 * listener en vivo. Idempotente: cada fila se aplica por `upsert` sobre su mismo id original,
 * así que se puede llamar varias veces sin duplicar nada — sirve tanto para "recuperar todo en
 * un celular nuevo" como para "traer lo que agregué desde otro dispositivo mientras tanto".
 * Ver `Plan/12-firebase-auth-y-sync.md`.
 */
class RestaurarDatosPersonalesUseCase @Inject constructor(
    private val personalSyncRepository: PersonalSyncRepository,
    private val actividadRepository: ActividadRepository,
    private val espacioRepository: EspacioRepository,
) {
    suspend operator fun invoke(usuarioId: String) {
        val espacioPersonalId = espacioRepository.obtenerEspacioPersonal(usuarioId)?.id ?: return

        personalSyncRepository.restaurarHabitos().forEach { (actividad, detalle) ->
            actividadRepository.mergeHabitoRemoto(actividad.copy(espacioId = espacioPersonalId), detalle)
        }
        personalSyncRepository.restaurarRegistrosHabito().forEach { registro ->
            actividadRepository.mergeRegistroHabitoRemoto(registro.actividadId, registro.fecha, registro.estado)
        }
        personalSyncRepository.restaurarTareas().forEach { (actividad, detalle) ->
            actividadRepository.mergeTareaRemota(actividad.copy(espacioId = espacioPersonalId), detalle)
        }
    }
}
