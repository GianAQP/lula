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

/** Acción registrada en `HistorialCambios` — ver lección de auditoría en el plan técnico. */
enum class AccionAuditoria { CREAR, ACTUALIZAR, ELIMINAR }

enum class OrigenCambio { LOCAL, SYNC }

enum class SyncStatus { LOCAL, PENDIENTE, SINCRONIZADO }
