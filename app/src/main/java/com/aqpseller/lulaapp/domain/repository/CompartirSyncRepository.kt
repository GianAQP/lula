package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Conexion
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Espejo en Firestore de lo mínimo que necesita viajar entre dos cuentas reales para que
 * Círculo de cuidado funcione — ver `Plan/12-firebase-auth-y-sync.md`. Solo entra acá lo que
 * involucra a otra persona (`SolicitudCompartir`, `Conexion`, un perfil mínimo de `Usuario`);
 * el resto de la app sigue 100% local. Todo método es "best effort": si Firestore no está
 * disponible o la cuenta no está vinculada todavía, quien llama debe tratarlo como no-op, nunca
 * como error que bloquea la acción local.
 */
interface CompartirSyncRepository {
    suspend fun subirPerfil(usuario: Usuario)
    suspend fun subirSolicitud(solicitud: SolicitudCompartir)
    suspend fun eliminarSolicitud(solicitudId: String)
    suspend fun subirConexion(conexion: Conexion)

    /** Escucha en vivo las solicitudes dirigidas a mi correo o que yo envié, para reflejar en
     * este dispositivo lo que pase del otro lado (alguien acepta/rechaza, o me llega una
     * nueva). Vacío mientras [miCorreo] esté en blanco (cuenta sin vincular todavía). */
    fun escucharSolicitudes(miUsuarioId: String, miCorreo: String): Flow<List<SolicitudCompartir>>
}
