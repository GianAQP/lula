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

/** Un día del historial de un Hábito personal tal como llega del respaldo en la nube. */
data class RegistroHabitoRemoto(val actividadId: String, val fecha: Long, val estado: EstadoActividad)

/**
 * Respaldo del Espacio Personal en Firestore — a diferencia de `EspacioSyncRepository`
 * (Familia, varias personas editando lo mismo, necesita escuchar en vivo), lo Personal es de un
 * solo dispositivo activo a la vez: alcanza con subir cada cambio (best-effort) y **restaurar
 * una sola vez** (al vincular la cuenta, o al abrir la app si ya estaba vinculada) en vez de un
 * listener permanente. Ver `Plan/12-firebase-auth-y-sync.md`.
 *
 * Alcance: Hábitos (con su historial día por día, la racha), Tareas, Finanzas, Diario, Notas,
 * Metas, Listas y Mi propósito — todo lo Personal salvo Medicamentos/Citas/Fechas importantes
 * (pendiente, mismo patrón cuando haga falta).
 */
interface PersonalSyncRepository {
    suspend fun subirHabito(actividad: Actividad, detalle: ActividadDetalle.Habito)
    suspend fun subirRegistroHabito(actividadId: String, fecha: Long, estado: EstadoActividad)
    suspend fun subirTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea)
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

    suspend fun restaurarHabitos(): List<Pair<Actividad, ActividadDetalle.Habito>>
    suspend fun restaurarRegistrosHabito(): List<RegistroHabitoRemoto>
    suspend fun restaurarTareas(): List<Pair<Actividad, ActividadDetalle.Tarea>>
    suspend fun restaurarMovimientosFinancieros(): List<MovimientoFinanciero>
    suspend fun restaurarEntradasDiario(): List<EntradaDiario>
    suspend fun restaurarNotas(): List<Nota>
    suspend fun restaurarMetas(): List<Meta>
    suspend fun restaurarListas(): List<ListaConItems>
    suspend fun restaurarProposito(): PropositoPersonal?
}
