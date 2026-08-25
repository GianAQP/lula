package com.aqpseller.lulaapp.data.repository

import com.aqpseller.lulaapp.domain.model.CanalEnvio
import com.aqpseller.lulaapp.domain.model.Conexion
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.TipoSolicitud
import com.aqpseller.lulaapp.domain.model.Usuario
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import com.aqpseller.lulaapp.domain.repository.PerfilRemoto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Filter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_USUARIOS = "usuarios"
private const val COLECCION_SOLICITUDES = "solicitudes_compartir"
private const val COLECCION_CONEXIONES = "conexiones"

/**
 * `de`/`para`/`usuarioA`/`usuarioB` son ids/contactos de la app (UUID local o texto libre), no
 * de Firebase — las reglas de seguridad de Firestore solo pueden verificar contra
 * `request.auth.uid`, así que cada escritura agrega también el uid real de quien la hace
 * (siempre "yo mismo", nunca en nombre de otro) para que las reglas puedan validarla. Ver
 * `Plan/12-firebase-auth-y-sync.md` y `firestore.rules` en la raíz del repo.
 */
class CompartirSyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : CompartirSyncRepository {

    override suspend fun subirPerfil(usuario: Usuario) {
        val uid = usuario.firebaseUid ?: return
        val datos = mapOf(
            "usuarioIdLocal" to usuario.id,
            "nombreCompleto" to usuario.nombreCompleto,
            "nombrePreferido" to usuario.nombrePreferido,
            "correo" to usuario.correo,
            "horaDesayuno" to usuario.horaDesayuno,
            "horaAlmuerzo" to usuario.horaAlmuerzo,
            "horaCena" to usuario.horaCena,
            "onboardingCompletadoEn" to usuario.onboardingCompletadoEn,
        )
        firestore.collection(COLECCION_USUARIOS).document(uid).set(datos).await()
    }

    override suspend fun restaurarPerfil(firebaseUid: String): PerfilRemoto? {
        val doc = firestore.collection(COLECCION_USUARIOS).document(firebaseUid).get().await()
        if (!doc.exists()) return null
        return PerfilRemoto(
            nombreCompleto = doc.getString("nombreCompleto"),
            nombrePreferido = doc.getString("nombrePreferido"),
            horaDesayuno = doc.getString("horaDesayuno"),
            horaAlmuerzo = doc.getString("horaAlmuerzo"),
            horaCena = doc.getString("horaCena"),
            onboardingCompletadoEn = doc.getLong("onboardingCompletadoEn"),
        )
    }

    override suspend fun subirSolicitud(solicitud: SolicitudCompartir) {
        val miFirebaseUid = firebaseAuth.currentUser?.uid ?: return
        val datos = mapOf(
            "de" to solicitud.de,
            "deFirebaseUid" to miFirebaseUid,
            "para" to solicitud.para,
            "tieneCuenta" to solicitud.tieneCuenta,
            "elementoId" to solicitud.elementoId,
            "contexto" to solicitud.contexto,
            "deNombre" to solicitud.deNombre,
            "tipoSolicitud" to solicitud.tipo.name,
            "permisos" to solicitud.permisos.name,
            "estado" to solicitud.estado.name,
            "canalEnvio" to solicitud.canalEnvio?.name,
            "fechaSolicitud" to solicitud.fechaSolicitud,
            "fechaRespuesta" to solicitud.fechaRespuesta,
        )
        firestore.collection(COLECCION_SOLICITUDES).document(solicitud.id).set(datos).await()
    }

    override suspend fun eliminarSolicitud(solicitudId: String) {
        firestore.collection(COLECCION_SOLICITUDES).document(solicitudId).delete().await()
    }

    override suspend fun subirConexion(conexion: Conexion) {
        if (firebaseAuth.currentUser == null) return
        val datos = mapOf(
            "usuarioA" to conexion.usuarioA,
            "usuarioB" to conexion.usuarioB,
            "tipo" to conexion.tipo.name,
            "origenSolicitudId" to conexion.origenSolicitudId,
            "fechaConexion" to conexion.fechaConexion,
        )
        firestore.collection(COLECCION_CONEXIONES).document(conexion.id).set(datos).await()
    }

    override fun escucharSolicitudes(miUsuarioId: String, miCorreo: String): Flow<List<SolicitudCompartir>> = callbackFlow {
        if (miCorreo.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(COLECCION_SOLICITUDES)
            .where(Filter.or(Filter.equalTo("para", miCorreo), Filter.equalTo("de", miUsuarioId)))

        val registro = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val solicitudes = snapshot.documents.mapNotNull { doc ->
                val de = doc.getString("de") ?: return@mapNotNull null
                val para = doc.getString("para") ?: return@mapNotNull null
                val elementoId = doc.getString("elementoId") ?: return@mapNotNull null
                SolicitudCompartir(
                    id = doc.id,
                    de = de,
                    para = para,
                    tieneCuenta = doc.getBoolean("tieneCuenta") ?: false,
                    elementoId = elementoId,
                    contexto = doc.getString("contexto") ?: "",
                    deNombre = doc.getString("deNombre") ?: "Alguien",
                    tipo = runCatching { TipoSolicitud.valueOf(doc.getString("tipoSolicitud") ?: "") }
                        .getOrDefault(TipoSolicitud.ACTIVIDAD),
                    permisos = runCatching { PermisoCompartir.valueOf(doc.getString("permisos") ?: "") }
                        .getOrDefault(PermisoCompartir.PUEDE_VER),
                    estado = runCatching { EstadoSolicitud.valueOf(doc.getString("estado") ?: "") }
                        .getOrDefault(EstadoSolicitud.PENDIENTE),
                    canalEnvio = doc.getString("canalEnvio")?.let { runCatching { CanalEnvio.valueOf(it) }.getOrNull() },
                    fechaSolicitud = doc.getLong("fechaSolicitud") ?: 0L,
                    fechaRespuesta = doc.getLong("fechaRespuesta"),
                )
            }
            trySend(solicitudes)
        }
        awaitClose { registro.remove() }
    }
}
