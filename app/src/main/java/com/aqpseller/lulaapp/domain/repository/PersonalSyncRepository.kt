package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad

/** Un día del historial de un Hábito personal tal como llega del respaldo en la nube. */
data class RegistroHabitoRemoto(val actividadId: String, val fecha: Long, val estado: EstadoActividad)

/**
 * Respaldo del Espacio Personal en Firestore — a diferencia de `EspacioSyncRepository`
 * (Familia, varias personas editando lo mismo, necesita escuchar en vivo), lo Personal es de un
 * solo dispositivo activo a la vez: alcanza con subir cada cambio (best-effort) y **restaurar
 * una sola vez** (al vincular la cuenta, o al abrir la app si ya estaba vinculada) en vez de un
 * listener permanente. Ver `Plan/12-firebase-auth-y-sync.md`.
 *
 * Alcance de esta ronda: Hábitos (con su historial día por día, la racha) y Tareas — lo que más
 * le dolería perder a alguien si cambia de celular. El resto (Medicamentos, Citas, Finanzas,
 * Diario, Notas, Mi propósito, Metas, Listas) queda pendiente para rondas siguientes.
 */
interface PersonalSyncRepository {
    suspend fun subirHabito(actividad: Actividad, detalle: ActividadDetalle.Habito)
    suspend fun subirRegistroHabito(actividadId: String, fecha: Long, estado: EstadoActividad)
    suspend fun subirTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea)

    suspend fun restaurarHabitos(): List<Pair<Actividad, ActividadDetalle.Habito>>
    suspend fun restaurarRegistrosHabito(): List<RegistroHabitoRemoto>
    suspend fun restaurarTareas(): List<Pair<Actividad, ActividadDetalle.Tarea>>
}
