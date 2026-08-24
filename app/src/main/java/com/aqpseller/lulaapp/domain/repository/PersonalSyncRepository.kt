package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EntradaDiario
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ListaConItems
import com.aqpseller.lulaapp.domain.model.Meta
import com.aqpseller.lulaapp.domain.model.MovimientoFinanciero
import com.aqpseller.lulaapp.domain.model.Nota
import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import com.aqpseller.lulaapp.domain.model.RegistroDiario
import com.aqpseller.lulaapp.domain.model.RegistroSemanal
import com.aqpseller.lulaapp.domain.model.SesionCita

/** Un día del historial de un Hábito personal tal como llega del respaldo en la nube. */
data class RegistroHabitoRemoto(val actividadId: String, val fecha: Long, val estado: EstadoActividad)

/** Una toma de un Medicamento tal como llega del respaldo en la nube — igual que
 * [RegistroHabitoRemoto] pero con `horario` (hay varias tomas por día, no una). */
data class TomaMedicamentoRemota(val actividadId: String, val fecha: Long, val horario: String, val estado: EstadoActividad)

/**
 * Respaldo del Espacio Personal en Firestore — a diferencia de `EspacioSyncRepository`
 * (Familia, varias personas editando lo mismo, necesita escuchar en vivo), lo Personal es de un
 * solo dispositivo activo a la vez: alcanza con subir cada cambio (best-effort) y **restaurar
 * una sola vez** (al vincular la cuenta, o al abrir la app si ya estaba vinculada) en vez de un
 * listener permanente. Ver `Plan/12-firebase-auth-y-sync.md`.
 *
 * Alcance: todo el Espacio Personal — Hábitos (con su historial día por día, la racha), Tareas,
 * Rutinas, Medicamentos (con sus tomas), Citas (con sus sesiones de curso), Fechas importantes,
 * Finanzas, Diario, Notas, Metas, Listas, Mi propósito, y el historial de "Cerrar mi día"/
 * Revisión semanal.
 */
interface PersonalSyncRepository {
    suspend fun subirHabito(actividad: Actividad, detalle: ActividadDetalle.Habito)
    suspend fun subirRegistroHabito(actividadId: String, fecha: Long, estado: EstadoActividad)
    suspend fun subirTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea)
    suspend fun subirRutina(actividad: Actividad, detalle: ActividadDetalle.Rutina)
    suspend fun subirMedicamento(actividad: Actividad, detalle: ActividadDetalle.Medicamento)
    suspend fun subirTomaMedicamento(actividadId: String, fecha: Long, horario: String, estado: EstadoActividad)
    suspend fun subirCita(actividad: Actividad, detalle: ActividadDetalle.Cita)
    suspend fun subirSesionCita(sesion: SesionCita)
    suspend fun subirFechaImportante(actividad: Actividad, detalle: ActividadDetalle.FechaImportante)
    /** Resube la actividad completa según su tipo (`actividad.detalle`) — para los flujos que no
     * conocen el tipo en tiempo de compilación (pausar/reanudar). No-op si `detalle` es null. */
    suspend fun subirActividadSegunTipo(actividad: Actividad)
    /** Borra el documento de la actividad y, si tenía, sus tomas/sesiones asociadas — evita que
     * una restauración futura reviva un registro huérfano sin su actividad dueña (violaría la FK
     * local). Cubre cualquier tipo (Hábito/Tarea/Rutina/Medicamento/Cita/Fecha importante), todos
     * viven en la misma colección. */
    suspend fun eliminarActividad(actividadId: String)
    suspend fun subirMovimientoFinanciero(movimiento: MovimientoFinanciero)
    suspend fun eliminarMovimientoFinanciero(movimientoId: String)
    suspend fun subirEntradaDiario(entrada: EntradaDiario)
    suspend fun eliminarEntradaDiario(entradaId: String)
    suspend fun subirNota(nota: Nota)
    suspend fun eliminarNota(notaId: String)
    suspend fun subirMeta(meta: Meta)
    suspend fun eliminarMeta(metaId: String)
    suspend fun subirLista(lista: ListaConItems)
    suspend fun eliminarLista(listaId: String)
    suspend fun subirProposito(proposito: PropositoPersonal)
    suspend fun subirRegistroDiario(registro: RegistroDiario)
    suspend fun subirRegistroSemanal(registro: RegistroSemanal)

    suspend fun restaurarHabitos(): List<Pair<Actividad, ActividadDetalle.Habito>>
    suspend fun restaurarRegistrosHabito(): List<RegistroHabitoRemoto>
    suspend fun restaurarTareas(): List<Pair<Actividad, ActividadDetalle.Tarea>>
    suspend fun restaurarRutinas(): List<Pair<Actividad, ActividadDetalle.Rutina>>
    suspend fun restaurarMedicamentos(): List<Pair<Actividad, ActividadDetalle.Medicamento>>
    suspend fun restaurarTomasMedicamento(): List<TomaMedicamentoRemota>
    suspend fun restaurarCitas(): List<Pair<Actividad, ActividadDetalle.Cita>>
    suspend fun restaurarSesionesCita(): List<SesionCita>
    suspend fun restaurarFechasImportantes(): List<Pair<Actividad, ActividadDetalle.FechaImportante>>
    suspend fun restaurarMovimientosFinancieros(): List<MovimientoFinanciero>
    suspend fun restaurarEntradasDiario(): List<EntradaDiario>
    suspend fun restaurarNotas(): List<Nota>
    suspend fun restaurarMetas(): List<Meta>
    suspend fun restaurarListas(): List<ListaConItems>
    suspend fun restaurarProposito(): PropositoPersonal?
    suspend fun restaurarRegistrosDiarios(): List<RegistroDiario>
    suspend fun restaurarRegistrosSemanales(): List<RegistroSemanal>
}
