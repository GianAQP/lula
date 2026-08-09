package com.aqpseller.lulaapp.domain.model

/** Nombres alineados con `Plan/03-vocabulario.md` — no renombrar sin actualizar el glosario. */

enum class TipoEspacio { PERSONAL, FAMILIA, EQUIPO }

enum class RolEnEspacio { ADMIN, MIEMBRO }

enum class TipoActividad { HABITO, TAREA, RUTINA, MEDICAMENTO, CITA, FECHA_IMPORTANTE }

/** Nunca interpretar SIN_CONFIRMAR como "no lo hizo" — ver `01-arquitectura.md`. */
enum class EstadoActividad { CONFIRMADO, SIN_CONFIRMAR, OMITIDO }

enum class Privacidad { SOLO_YO, COMPARTIDO, FAMILIA, GRUPO }

enum class MomentoDelDia { MANANA, TARDE, NOCHE }

enum class FrecuenciaHabito { DIARIA, DIAS_ESPECIFICOS }

enum class MetodoLogin { GOOGLE, CORREO_MAGICO, LOCAL }

enum class TipoMovimientoFinanciero { INGRESO, EGRESO }

enum class ComoSeMideMeta { POR_HABITO, POR_MONTO, POR_NUMERO, MANUAL }

enum class ModoFrecuenciaMedicamento { INTERVALO_HORAS, RELACION_COMIDA }

enum class Comida { DESAYUNO, ALMUERZO, CENA }

enum class MomentoRelativoComida { ANTES, DESPUES }

enum class Recurrencia { UNICA, SEMANAL, ANUAL }

enum class AnticipacionRecordatorio { MISMO_DIA, UN_DIA_ANTES, UNA_SEMANA_ANTES }

enum class TipoAviso { ALARMA_SONORA, MENSAJE_SILENCIOSO }

/** Intensidad del recordatorio de un Hábito/Tarea — elegible por el usuario, nunca decidido por Lula. */
enum class NivelRecordatorio { SILENCIOSO, SONIDO, ALARMA }

/**
 * Cada cuánto se repite una Tarea (pagar la luz, agua, etc.) — a diferencia de `Recurrencia`
 * (solo para Fecha importante). Al marcarla `CONFIRMADO`, se registra en el historial y se
 * reprograma sola para la próxima fecha, igual que un Hábito. Ver `08-decisiones-tecnicas.md`.
 */
enum class RecurrenciaTarea { SIN_REPETIR, DIARIA, SEMANAL, QUINCENAL, MENSUAL, BIMESTRAL, TRIMESTRAL, ANUAL }

/** Acción registrada en `HistorialCambios` — ver lección de auditoría en el plan técnico. */
enum class AccionAuditoria { CREAR, ACTUALIZAR, ELIMINAR }

/** Qué puede hacer la persona con quien se comparte una actividad — nunca más de esto. */
enum class PermisoCompartir { PUEDE_VER, PUEDE_VER_Y_RECORDAR }

/** Ver `Plan/01-arquitectura.md` — compartir siempre es solicitud + aceptación, nunca automático. */
enum class EstadoSolicitud { PENDIENTE, ACEPTADA, RECHAZADA, ESPERANDO_INSTALACION }

enum class CanalEnvio { CORREO, WHATSAPP, SMS }

enum class OrigenCambio { LOCAL, SYNC }

enum class SyncStatus { LOCAL, PENDIENTE, SINCRONIZADO }

/** Solo Diaria/Semanal, a propósito más simple que `RecurrenciaTarea` — ver `02-pantallas.md`. */
enum class FrecuenciaRetoFamiliar { DIARIA, SEMANAL }
