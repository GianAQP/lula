package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.DiaHistorialHabito
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.EstadoActividadEnFecha
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.SesionCita
import com.aqpseller.lulaapp.domain.model.TomaMedicamento
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ActividadRepository {
    suspend fun crearHabito(actividad: Actividad, detalle: ActividadDetalle.Habito, usuarioId: String)
    suspend fun crearTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea, usuarioId: String)
    suspend fun crearRutina(actividad: Actividad, detalle: ActividadDetalle.Rutina, usuarioId: String)
    suspend fun crearMedicamento(actividad: Actividad, detalle: ActividadDetalle.Medicamento, usuarioId: String)
    suspend fun crearCita(actividad: Actividad, detalle: ActividadDetalle.Cita, usuarioId: String)
    suspend fun crearFechaImportante(actividad: Actividad, detalle: ActividadDetalle.FechaImportante, usuarioId: String)
    /** [fechaCompletado] solo aplica a Tarea (ver `ActividadRepositoryImpl.marcarEstado`) — permite
     * "completado el {fecha}" con una fecha puntual en vez de siempre "ahora", para marcar desde
     * Calendario una tarea que se hizo un día distinto al de hoy. Null (default) = ahora mismo. */
    suspend fun marcarEstado(id: String, estado: EstadoActividad, usuarioId: String, fechaCompletado: Long? = null)

    suspend fun actualizarHabito(actividadId: String, nombre: String, detalle: ActividadDetalle.Habito, usuarioId: String)
    suspend fun actualizarTarea(actividadId: String, nombre: String, detalle: ActividadDetalle.Tarea, usuarioId: String)
    suspend fun actualizarRutina(actividadId: String, nombre: String, detalle: ActividadDetalle.Rutina, usuarioId: String)
    suspend fun actualizarMedicamento(actividadId: String, nombre: String, detalle: ActividadDetalle.Medicamento, usuarioId: String)
    suspend fun actualizarCita(actividadId: String, nombre: String, detalle: ActividadDetalle.Cita, usuarioId: String)
    suspend fun actualizarFechaImportante(actividadId: String, nombre: String, detalle: ActividadDetalle.FechaImportante, usuarioId: String)
    suspend fun pausarReanudar(actividadId: String, activa: Boolean, usuarioId: String)
    suspend fun eliminar(actividadId: String, usuarioId: String)

    /**
     * Avanza una Tarea recurrente a su próxima fecha: registra la ocurrencia actual en el
     * historial, actualiza `fechaLimite` y vuelve a dejarla `SIN_CONFIRMAR`. Ver
     * `08-decisiones-tecnicas.md`.
     */
    suspend fun reprogramarTareaRecurrente(actividadId: String, nuevaFechaLimite: Long, usuarioId: String)

    /** Responder la tarjeta de progresión de un Hábito (subir/mantener/recordarme después). */
    suspend fun actualizarProgresionHabito(actividadId: String, duracionActualMin: Int, proximaRevisionEpochDay: Long, usuarioId: String)

    suspend fun obtenerConDetalle(actividadId: String): Actividad?

    /** Tareas que acompañan a un Medicamento/Cita (ver `actividadVinculadaId` en `ActividadDetalle.Tarea`). */
    suspend fun obtenerTareasVinculadasA(actividadVinculadaId: String): List<Actividad>

    /** Trae actividades activas del espacio con su detalle (Habito/Tarea/Rutina) ya resuelto. */
    fun observarActividadesDeEspacio(espacioId: String): Flow<List<Actividad>>
    fun observarHabitos(espacioId: String): Flow<List<Actividad>>
    fun observarTareas(espacioId: String): Flow<List<Actividad>>
    fun observarRutinas(espacioId: String): Flow<List<Actividad>>
    fun observarMedicamentos(espacioId: String): Flow<List<Actividad>>
    fun observarCitas(espacioId: String): Flow<List<Actividad>>
    fun observarFechasImportantes(espacioId: String): Flow<List<Actividad>>

    /** Últimos [dias] días de cumplimiento de un hábito, ordenados de más antiguo a más reciente. */
    suspend fun obtenerHistorialHabito(actividadId: String, dias: Int): List<DiaHistorialHabito>

    /**
     * Marca una toma puntual de un Medicamento — a diferencia de `marcarEstado`, necesita
     * `horario` porque hay varias tomas por día ("uno por horario, no uno por día", ver
     * `Plan/01-arquitectura.md`).
     */
    suspend fun marcarToma(actividadId: String, horario: String, estado: EstadoActividad, usuarioId: String)

    /** Tomas de hoy de los medicamentos indicados — usado por Hoy y "Mi salud". */
    fun observarTomasDeHoy(actividadIds: List<String>): Flow<List<TomaMedicamento>>

    /** Últimos [dias] días de tomas de un medicamento, para el historial semanal de su detalle. */
    suspend fun obtenerHistorialTomas(actividadId: String, dias: Int): List<TomaMedicamento>

    /** Estados de varios Hábitos en un rango de fechas (epoch day) — usado por el Calendario. */
    suspend fun obtenerEstadosDeRango(actividadIds: List<String>, desde: Long, hasta: Long): List<EstadoActividadEnFecha>

    /** Tomas de varios Medicamentos en un rango de fechas (epoch day) — usado por el Calendario. */
    suspend fun obtenerTomasDeRango(actividadIds: List<String>, desde: Long, hasta: Long): List<TomaMedicamento>

    /** Estado actual de una toma puntual — null si todavía no se creó ninguna fila (equivale a
     * SIN_CONFIRMAR). Usado por el recordatorio persistente para saber si todavía hace falta
     * seguir insistiendo. */
    suspend fun obtenerEstadoToma(actividadId: String, fecha: Long, horario: String): EstadoActividad?

    /** Reemplaza (upsert) el set completo de sesiones de un curso de Cita — usado tanto al
     * generar sesiones nuevas como al reprogramar una existente (mismo `numeroSesion`). */
    suspend fun guardarSesionesCita(sesiones: List<SesionCita>)

    suspend fun obtenerSesionesCita(actividadId: String): List<SesionCita>

    /** Sesiones de curso de varias Citas en un rango de fechas (epoch day) — usado por el Calendario/Hoy. */
    suspend fun obtenerSesionesCitaDeRango(actividadIds: List<String>, desde: Long, hasta: Long): List<SesionCita>

    suspend fun marcarSesionCita(actividadId: String, numeroSesion: Int, estado: EstadoActividad, usuarioId: String)

    /** Reprograma UNA sesión puntual — no toca `fechaOriginal` ni el resto del curso (ver `08-decisiones-tecnicas.md`). */
    suspend fun reprogramarSesionCita(actividadId: String, numeroSesion: Int, nuevaFecha: Long, usuarioId: String)

    /**
     * Reconstruye, desde `historial_cambios`, qué actividades del espacio se eliminaron en
     * [desde]..[hasta] — cada una devuelta con la fecha en que se eliminó (no su fecha original)
     * y marcada `ItemAgenda.eliminado = true`. Usado por el Calendario para dejar rastro de lo
     * borrado en vez de que desaparezca sin dejar huella. Ver `Plan/08-decisiones-tecnicas.md`.
     */
    suspend fun obtenerEliminadosDeRango(espacioId: String, desde: LocalDate, hasta: LocalDate): List<Pair<LocalDate, ItemAgenda>>
}
