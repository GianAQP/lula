package com.aqpseller.lulaapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Entidad genérica central del dominio (ver regla no negociable en `Plan/01-arquitectura.md`):
 * representa hábito, tarea, rutina, medicamento, cita o fecha importante. Todo módulo nuevo
 * debe reutilizar esta estructura, nunca crear un modelo paralelo.
 *
 * `momentoDelDia` y `areaDeVidaId` están denormalizados aquí (ver `08-decisiones-tecnicas.md`)
 * para que la consulta de Hoy no necesite hacer join con las tablas de detalle.
 */
data class Actividad(
    val id: String,
    val tipo: TipoActividad,
    val espacioId: String,
    val nombre: String,
    val propietario: String,
    val responsables: List<String>,
    val puedeVer: List<String>,
    val puedeRecordar: List<String>,
    val estado: EstadoActividad,
    val privacidad: Privacidad,
    val syncStatus: SyncStatus,
    val esPremiumFeature: Boolean,
    val areaDeVidaId: String?,
    val momentoDelDia: MomentoDelDia?,
    val fechaCreacion: Long,
    val activa: Boolean,
    val detalle: ActividadDetalle?,
    /** Solo para Tarea puntual: momento en que `estado` pasó a CONFIRMADO. Null si no está completada. */
    val fechaCompletado: Long? = null,
)

sealed interface ActividadDetalle {

    data class Habito(
        val momentoDelDia: MomentoDelDia,
        val frecuencia: FrecuenciaHabito,
        val diasEspecificos: List<Int> = emptyList(),
        val duracionInicialMin: Int? = null,
        val duracionObjetivoMin: Int? = null,
        val incrementoMin: Int? = null,
        val frecuenciaRevisionDias: Int? = null,
        /** Formato "HH:mm", null = sin recordatorio. */
        val horaRecordatorio: String? = null,
        val nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        /** Duración que se usa hoy — arranca en `duracionInicialMin`, sube con cada revisión aceptada. */
        val duracionActualMin: Int? = null,
        /** Epoch day en que toca preguntar "¿aumentamos?" — null si no es un hábito progresivo. */
        val proximaRevisionEpochDay: Long? = null,
    ) : ActividadDetalle {
        /** Progresivo = el usuario configuró los 4 campos necesarios para que Lula pregunte. */
        val esProgresivo: Boolean
            get() = duracionInicialMin != null && duracionObjetivoMin != null && incrementoMin != null && frecuenciaRevisionDias != null
    }

    data class Tarea(
        val fechaLimite: Long? = null,
        val prioridad: Int? = null,
        val importante: Boolean = false,
        val urgente: Boolean = false,
        /** Formato "HH:mm", solo tiene efecto si hay `fechaLimite`. */
        val horaRecordatorio: String? = null,
        val nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        /** Solo tiene efecto si hay `fechaLimite` — sin fecha no hay desde dónde avanzar. */
        val recurrencia: RecurrenciaTarea = RecurrenciaTarea.SIN_REPETIR,
        /** Medicamento o Cita al que esta tarea acompaña (ej. "cuidar a alguien por un tiempo") —
         * ver `08-decisiones-tecnicas.md`. Cuando esa actividad termina su ciclo de vida, esta
         * tarea se cierra sola. */
        val actividadVinculadaId: String? = null,
    ) : ActividadDetalle

    data class Rutina(
        val actividadesIncluidasIds: List<String>,
        val momentoDelDia: MomentoDelDia,
    ) : ActividadDetalle

    data class Medicamento(
        val nombreMedicamento: String,
        val dosis: String,
        val modoFrecuencia: ModoFrecuenciaMedicamento,
        val intervaloHoras: Int? = null,
        val horaPrimeraDosis: String? = null,
        /** Formato "HH:mm" — calculados desde `intervaloHoras`/`horaPrimeraDosis` o desde `comidasRelacionadas`. */
        val horariosCalculados: List<String> = emptyList(),
        val comidasRelacionadas: List<ComidaRelacionada> = emptyList(),
        val fechaInicio: Long,
        val fechaFin: Long? = null,
        /** Si se usó "cantidad de dosis" para calcular `fechaFin` en vez de elegir una fecha a
         * mano — permite recortar las tomas sobrantes del último día (ver `HorariosMedicamentoUtils`). */
        val cantidadDosisTotal: Int? = null,
        val nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        /** Si además de sonar una vez, debe seguir insistiendo cada [intervaloPersistenciaMin]
         * minutos hasta que se marque la toma (o termine el día) — ver `RecordatorioReceiver`. */
        val recordatorioPersistente: Boolean = false,
        val intervaloPersistenciaMin: Int? = null,
    ) : ActividadDetalle

    data class Cita(
        val lugar: String? = null,
        val motivo: String? = null,
        /** Ignorado si `esCurso` — una Cita de curso no tiene una sola fecha, tiene `SesionCita`. */
        val fechaHora: Long,
        /** Uno o más recordatorios, cada uno con su propia hora — ej. "un día antes a las
         * 20:00" y "el mismo día a las 7:00", no necesariamente a la misma hora de la cita.
         * Para un curso, se reutilizan igual para cada sesión. */
        val recordatorios: List<RecordatorioCita> = emptyList(),
        val nivelRecordatorio: NivelRecordatorio = NivelRecordatorio.SONIDO,
        /** "Curso" = varias sesiones (ej. radioterapia, masajes) en vez de una cita puntual —
         * ver `08-decisiones-tecnicas.md`. Las sesiones viven en `SesionCita`, no acá. */
        val esCurso: Boolean = false,
        /** Días ISO (1=lunes..7=domingo) en que cae cada sesión — vigente desde ahora hacia
         * adelante; cambiarlo no toca sesiones ya generadas (ver decisión de "tramos"). */
        val diasSemana: Set<Int> = emptySet(),
        /** Formato "HH:mm", hora de cada sesión del curso. */
        val horaSesion: String? = null,
        val fechaInicioCurso: Long? = null,
        /** Null = sin cantidad fija (curso abierto, ej. masajes sin dosis definida). */
        val cantidadSesionesTotal: Int? = null,
    ) : ActividadDetalle

    data class FechaImportante(
        val recurrencia: Recurrencia,
        val fechaBase: Long,
        val horaNotificacion: String,
        val anticipacion: AnticipacionRecordatorio,
        val tipoAviso: TipoAviso,
    ) : ActividadDetalle
}

/** Un día del historial de cumplimiento de un hábito (ver `RegistroActividadEntity`). */
data class DiaHistorialHabito(
    val fecha: Long,
    val estado: EstadoActividad,
)

/** Ej. "después del almuerzo" — la instrucción original siempre se muestra, no solo la hora calculada. */
@Serializable
data class ComidaRelacionada(
    val comida: Comida,
    val momento: MomentoRelativoComida,
)

/** Un recordatorio de Cita: cuántos días antes + a qué hora propia suena (no la hora de la cita). */
@Serializable
data class RecordatorioCita(
    val anticipacion: AnticipacionRecordatorio,
    /** Formato "HH:mm". */
    val hora: String,
)

/** Una toma de un Medicamento — a diferencia de un Hábito, hay varias por día ("uno por horario, no uno por día"). */
data class TomaMedicamento(
    val id: String,
    val actividadId: String,
    val fecha: Long,
    val horario: String,
    val estado: EstadoActividad,
)

/**
 * Una sesión de una Cita de curso (`ActividadDetalle.Cita.esCurso`) — ej. sesión 7 de 20 de
 * radioterapia. `numeroSesion` es fijo (el orden que se muestra) aunque `fecha` cambie por una
 * reprogramación puntual; `fechaOriginal` guarda la fecha que le tocaba según el patrón, solo
 * como referencia. Ver `08-decisiones-tecnicas.md`.
 */
data class SesionCita(
    val id: String,
    val actividadId: String,
    val numeroSesion: Int,
    val fecha: Long,
    val fechaOriginal: Long,
    val horario: String,
    val estado: EstadoActividad,
)

/** Estado de un Hábito en un día puntual (epoch day) — usado para armar el Calendario sobre un rango de fechas. */
data class EstadoActividadEnFecha(
    val actividadId: String,
    val fecha: Long,
    val estado: EstadoActividad,
)

/** Una dosis esperada hoy de un Medicamento, con su estado actual (`SIN_CONFIRMAR` si aún no se registró ninguna toma). */
data class TomaDeHoy(
    val horario: String,
    val instruccion: String,
    val estado: EstadoActividad,
)

/** Un Medicamento activo con sus tomas de hoy ya resueltas — usado por Hoy y "Mi salud". */
data class MedicamentoDeHoy(
    val actividadId: String,
    val nombre: String,
    val dosis: String,
    val nivelRecordatorio: NivelRecordatorio,
    val tomas: List<TomaDeHoy>,
)

