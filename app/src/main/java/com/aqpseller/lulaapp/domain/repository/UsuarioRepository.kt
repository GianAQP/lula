package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun observarUsuario(): Flow<Usuario?>
    suspend fun contarUsuarios(): Int
    suspend fun crearUsuario(usuario: Usuario)

    /** Para calcular horarios de medicamentos "según las comidas" — ver `Plan/08-decisiones-tecnicas.md`. */
    suspend fun actualizarHorariosComida(usuarioId: String, horaDesayuno: String?, horaAlmuerzo: String?, horaCena: String?)

    /** Ver `Plan/11-cuentas-y-conexiones.md` — consentimientos de cuenta, no de una sola vez:
     * cada parámetro null/false deja el valor actual sin tocar (para poder aceptar de a uno). */
    suspend fun actualizarConsentimientos(
        usuarioId: String,
        confirmoMayorDe13: Boolean? = null,
        terminosAceptadosEn: Long? = null,
        consentimientoDatosSaludEn: Long? = null,
    )

    /** Borra TODA la base de datos local — hoy equivale a borrar la cuenta entera, porque solo
     * hay un usuario semilla. Ver `Plan/11-cuentas-y-conexiones.md`. */
    suspend fun eliminarCuenta()

    /** "Reclamar cuenta": vincula el usuario semilla local con una cuenta real de Google —
     * actualiza la misma fila (mismo `id`), nunca crea una nueva ni toca ningún FK. Ver
     * `Plan/12-firebase-auth-y-sync.md`, sección 5. */
    suspend fun vincularConGoogle(usuarioId: String, correo: String, firebaseUid: String)
}
