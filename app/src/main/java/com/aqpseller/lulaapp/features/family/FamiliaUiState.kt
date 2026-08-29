package com.aqpseller.lulaapp.features.family

data class EspacioUi(
    val id: String,
    val nombre: String,
    val esFamilia: Boolean,
    val esActivo: Boolean,
)

/** Una fila de la lista "Tus espacios familiares" — puede haber varias (la que formaste, la de
 * tus padres, la de tu pareja...), cada una independiente. */
data class FamiliaResumenUi(
    val id: String,
    val nombre: String,
    /** usuarioId de quien la creó — para saber quién conserva el privilegio de eliminarla. */
    val creadoPor: String,
)

data class MiembroUi(
    val usuarioId: String,
    val firebaseUid: String?,
    val nombre: String,
    val rol: String,
    val esUnoMismo: Boolean,
    /** Quien creó el espacio no lo puede sacar otro admin — solo puede salir él mismo. */
    val esCreador: Boolean,
)

data class HistorialEventoUi(
    val fecha: String,
    val texto: String,
)

data class FamiliaUiState(
    val cargando: Boolean = true,
    val espacios: List<EspacioUi> = emptyList(),
    /** Todas mis Familias — un usuario puede tener varias (la que formó, la de sus padres, la de
     * su pareja), cada una con sus propios miembros y contenido. */
    val familias: List<FamiliaResumenUi> = emptyList(),
    /** Cuál Familia se está viendo/administrando ahora — independiente de cuál es el "espacio
     * activo" de arriba (administrar una Familia no te cambia de espacio de trabajo). */
    val familiaSeleccionadaId: String? = null,
    val nombreEspacioFamilia: String = "",
    val miembros: List<MiembroUi> = emptyList(),
    /** Admin (puede haber varios — co-admins) — agregar/quitar miembros, hacer admin a otro. */
    val soyAdmin: Boolean = false,
    /** Solo quien creó el espacio — además de todo lo de admin, puede eliminar el espacio
     * completo y no puede ser quitado por otro admin. */
    val soyCreador: Boolean = false,
    val mostrarFormularioCrear: Boolean = false,
    val mostrarFormularioRenombrar: Boolean = false,
    val mostrarFormularioInvitar: Boolean = false,
    /** Requiere que la cuenta esté vinculada con Google (Perfil → "🔑 Cuenta") para poder
     * invitar de verdad — ver `Plan/12-firebase-auth-y-sync.md`. */
    val cuentaVinculada: Boolean = false,
    /** Se puso true tras crear o cambiar de espacio — la pantalla vuelve sola a Hoy. */
    val espacioCambiado: Boolean = false,
    /** true tras enviar una invitación — muestra el QR + botón de WhatsApp de la invitación. */
    val mostrarInvitacionEnviada: Boolean = false,
    /** Código de invitación de corta duración — escanearlo une a la persona de inmediato, sin
     * paso de aceptar aparte. Se renueva solo mientras el diálogo sigue abierto. */
    val mostrarCodigoQr: Boolean = false,
    val codigoQrTexto: String? = null,
    /** Solo visible para admins — quién quitó a quién de este espacio. */
    val mostrarHistorial: Boolean = false,
    val historial: List<HistorialEventoUi> = emptyList(),
)
