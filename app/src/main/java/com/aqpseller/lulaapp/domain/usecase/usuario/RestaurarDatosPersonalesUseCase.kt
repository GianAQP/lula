package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import javax.inject.Inject

/**
 * Restaura el respaldo personal desde Firestore — pensado para correr UNA VEZ (al vincular la
 * cuenta con Google, o al abrir la app si ya estaba vinculada), no un listener en vivo.
 * Idempotente: cada fila se aplica por `upsert` sobre su mismo id original, así que se puede
 * llamar varias veces sin duplicar nada — sirve tanto para "recuperar todo en un celular nuevo"
 * como para "traer lo que agregué desde otro dispositivo mientras tanto". Ver
 * `Plan/12-firebase-auth-y-sync.md`.
 */
class RestaurarDatosPersonalesUseCase @Inject constructor(
    private val personalSyncRepository: PersonalSyncRepository,
    private val actividadRepository: ActividadRepository,
    private val espacioRepository: EspacioRepository,
    private val finanzasRepository: FinanzasRepository,
    private val entradaDiarioRepository: EntradaDiarioRepository,
    private val notaRepository: NotaRepository,
    private val metaRepository: MetaRepository,
    private val listaRepository: ListaRepository,
    private val propositoPersonalRepository: PropositoPersonalRepository,
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
        personalSyncRepository.restaurarMovimientosFinancieros().forEach { movimiento ->
            finanzasRepository.registrarMovimiento(movimiento.copy(espacioId = espacioPersonalId), usuarioId)
        }
        personalSyncRepository.restaurarEntradasDiario().forEach { entrada ->
            entradaDiarioRepository.crear(entrada.copy(espacioId = espacioPersonalId))
        }
        personalSyncRepository.restaurarNotas().forEach { nota ->
            notaRepository.crear(nota.copy(espacioId = espacioPersonalId))
        }
        personalSyncRepository.restaurarMetas().forEach { meta ->
            metaRepository.crear(meta.copy(espacioId = espacioPersonalId), usuarioId)
        }
        personalSyncRepository.restaurarListas().forEach { lista ->
            listaRepository.mergeRemota(espacioPersonalId, lista, usuarioId)
        }
        personalSyncRepository.restaurarProposito()?.let { proposito ->
            proposito.respuestas.forEach { (preguntaId, respuesta) ->
                propositoPersonalRepository.guardarRespuesta(espacioPersonalId, usuarioId, preguntaId, respuesta)
            }
        }
    }
}
