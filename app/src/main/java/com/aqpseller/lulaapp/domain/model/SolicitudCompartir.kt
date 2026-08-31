package com.aqpseller.lulaapp.domain.model

/**
 * Compartir siempre es solicitud + aceptación, nunca automático (ver `Plan/01-arquitectura.md`):
 * se crea esta solicitud, y solo cuando `para` la acepta se agrega a `puedeVer[]`/
 * `puedeRecordar[]` de la `Actividad` referenciada por `elementoId`. Esa aceptación real
 * todavía no está conectada (necesita que la otra persona tenga cuenta y la app instalada,
 * ver `Plan/08-decisiones-tecnicas.md`) — por ahora toda solicitud creada queda `PENDIENTE`.
 */
data class SolicitudCompartir(
    val id: String,
    /** usuarioId de quien comparte. */
    val de: String,
    /** Contacto (correo/teléfono) de quien recibe — todavía no hay búsqueda de cuentas reales. */
    val para: String,
    val tieneCuenta: Boolean,
    /** actividadId del elemento compartido. */
    /** actividadId si [tipo] es ACTIVIDAD, espacioId si es ESPACIO. */
    val elementoId: String,
    /** Nombre del elemento, denormalizado para mostrar sin resolver un join. */
    val contexto: String,
    /** Nombre de quien envía, denormalizado — para mostrarlo del lado del destinatario sin
     * tener que resolver su perfil. */
    val deNombre: String,
    val tipo: TipoSolicitud = TipoSolicitud.ACTIVIDAD,
    /** Solo tiene sentido si [tipo] es ACTIVIDAD — para ESPACIO, aceptar siempre agrega como
     * `MIEMBRO` normal (ver `AceptarSolicitudCompartirUseCase`). */
    val permisos: PermisoCompartir,
    val estado: EstadoSolicitud,
    val canalEnvio: CanalEnvio?,
    val fechaSolicitud: Long,
    val fechaRespuesta: Long? = null,
    /** Nombre de quien aceptó/rechazó, denormalizado igual que [deNombre] — para poder avisarle
     * a quien envió con un nombre real en vez de mostrar `para` (que es solo un correo/teléfono).
     * Ver `Plan/08-decisiones-tecnicas.md`. */
    val nombreQuienResponde: String? = null,
)
