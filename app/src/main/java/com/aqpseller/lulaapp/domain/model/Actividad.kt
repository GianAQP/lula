package com.aqpseller.lulaapp.domain.model

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
    val detalle: ActividadDetalle?,
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
    ) : ActividadDetalle

    data class Tarea(
        val fechaLimite: Long? = null,
        val prioridad: Int? = null,
        val importante: Boolean = false,
        val urgente: Boolean = false,
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
        val horariosCalculados: List<String> = emptyList(),
        val comidasRelacionadas: List<Comida> = emptyList(),
        val fechaInicio: Long,
        val fechaFin: Long? = null,
    ) : ActividadDetalle

    data class Cita(
        val lugar: String? = null,
        val motivo: String? = null,
        val fechaHora: Long,
        val recordatorioAnticipacion: AnticipacionRecordatorio,
    ) : ActividadDetalle

    data class FechaImportante(
        val recurrencia: Recurrencia,
        val fechaBase: Long,
        val horaNotificacion: String,
        val anticipacion: AnticipacionRecordatorio,
        val tipoAviso: TipoAviso,
    ) : ActividadDetalle
}
