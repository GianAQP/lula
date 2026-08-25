package com.aqpseller.lulaapp.features.family

data class EspacioUi(
    val id: String,
    val nombre: String,
    val esFamilia: Boolean,
    val esActivo: Boolean,
)

data class MiembroUi(
    val usuarioId: String,
    val firebaseUid: String?,
    val nombre: String,
    val rol: String,
    val esUnoMismo: Boolean,
)

data class FamiliaUiState(
    val cargando: Boolean = true,
    val espacios: List<EspacioUi> = emptyList(),
    val espacioFamiliaId: String? = null,
    val nombreEspacioFamilia: String = "",
    val miembros: List<MiembroUi> = emptyList(),
    /** Solo el admin puede quitar a otros miembros. */
    val soyAdmin: Boolean = false,
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
)
