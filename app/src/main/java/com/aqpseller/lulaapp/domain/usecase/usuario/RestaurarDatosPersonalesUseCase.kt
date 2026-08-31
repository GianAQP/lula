package com.aqpseller.lulaapp.domain.usecase.usuario

import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import com.aqpseller.lulaapp.domain.repository.NotificacionRepository
import com.aqpseller.lulaapp.domain.repository.PersonalSyncRepository
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import com.aqpseller.lulaapp.domain.repository.RegistroSemanalRepository
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
    private val registroDiarioRepository: RegistroDiarioRepository,
    private val registroSemanalRepository: RegistroSemanalRepository,
    private val notificacionRepository: NotificacionRepository,
) {
    suspend operator fun invoke(usuarioId: String) {
        val espacioPersonalId = espacioRepository.obtenerEspacioPersonal(usuarioId)?.id ?: return

        // Actividades primero, sus registros/tomas/sesiones después — referencian la actividad
        // por FK local (CASCADE), restaurar en el orden contrario rompería la restauración.
        personalSyncRepository.restaurarHabitos().forEach { (actividad, detalle) ->
            actividadRepository.mergeHabitoRemoto(actividad.copy(espacioId = espacioPersonalId), detalle)
        }
        personalSyncRepository.restaurarRegistrosHabito().forEach { registro ->
            actividadRepository.mergeRegistroHabitoRemoto(registro.actividadId, registro.fecha, registro.estado)
        }
        personalSyncRepository.restaurarTareas().forEach { (actividad, detalle) ->
            actividadRepository.mergeTareaRemota(actividad.copy(espacioId = espacioPersonalId), detalle)
        }
        personalSyncRepository.restaurarRutinas().forEach { (actividad, detalle) ->
            actividadRepository.mergeRutinaRemota(actividad.copy(espacioId = espacioPersonalId), detalle)
        }
        personalSyncRepository.restaurarMedicamentos().forEach { (actividad, detalle) ->
            actividadRepository.mergeMedicamentoRemoto(actividad.copy(espacioId = espacioPersonalId), detalle)
        }
        personalSyncRepository.restaurarTomasMedicamento().forEach { toma ->
            actividadRepository.mergeTomaMedicamentoRemota(toma.actividadId, toma.fecha, toma.horario, toma.estado)
        }
        personalSyncRepository.restaurarCitas().forEach { (actividad, detalle) ->
            actividadRepository.mergeCitaRemota(actividad.copy(espacioId = espacioPersonalId), detalle)
        }
        personalSyncRepository.restaurarSesionesCita().forEach { sesion ->
            actividadRepository.guardarSesionesCita(listOf(sesion))
        }
        personalSyncRepository.restaurarFechasImportantes().forEach { (actividad, detalle) ->
            actividadRepository.mergeFechaImportanteRemota(actividad.copy(espacioId = espacioPersonalId), detalle)
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
        personalSyncRepository.restaurarRegistrosDiarios().forEach { registro ->
            registroDiarioRepository.cerrarDia(registro.copy(espacioId = espacioPersonalId), usuarioId)
        }
        personalSyncRepository.restaurarRegistrosSemanales().forEach { registro ->
            registroSemanalRepository.guardarRevision(registro.copy(espacioId = espacioPersonalId), usuarioId)
        }
        personalSyncRepository.restaurarNotificaciones().forEach { notificacion ->
            notificacionRepository.restaurarDesdeRemota(notificacion)
        }
    }
}
