package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Conexion
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.SolicitudCompartir
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/** Perfil tal como llega del respaldo en la nube — usado para recuperarlo en un celular nuevo
 * (nombre real y si el registro ya se completó, para no volver a pedirlo). Ver
 * `ReclamarCuentaConGoogleUseCase`. */
data class PerfilRemoto(
    val nombreCompleto: String?,
    val nombrePreferido: String?,
    val horaDesayuno: String?,
    val horaAlmuerzo: String?,
    val horaCena: String?,
    val onboardingCompletadoEn: Long?,
)

/** Ajustes de pantalla/preferencias — a diferencia de todo lo demás en `CompartirSyncRepository`
 * (que involucra a otra persona), esto es 100% mío, pero el usuario pidió explícitamente que no
 * se pierda al cambiar de celular. Se restaura una sola vez, al vincular la cuenta (ver
 * `ReclamarCuentaConGoogleUseCase`) — no en cada apertura de la app, para no pisar un cambio
 * reciente hecho en ESTE dispositivo con una copia vieja de otro. Deliberadamente NO incluye
 * `espacioActivoId` (vive solo en memoria a propósito, ver `AjustesRepositoryImpl`) ni
 * `ultimoHitoRachaCelebrado` (no es una preferencia real, es solo para no repetir una
 * celebración). Ver `Plan/08-decisiones-tecnicas.md`. */
/** Un "recordarle" pedido por quien acompaña (permiso `PUEDE_VER_Y_RECORDAR`) — no es una
 * notificación push real (esta app no tiene esa infraestructura), es un aviso que se muestra si
 * el celular de quien comparte tiene la app abierta/en memoria cuando llega, mismo criterio
 * "best-effort" que el resto de esta sincronización. Ver `Plan/08-decisiones-tecnicas.md`. */
data class RecordatorioSolicitado(
    val id: String,
    val actividadId: String,
    val nombreActividad: String,
    val deNombre: String,
)

data class AjustesRemotos(
    val sonidoCheckHabilitado: Boolean?,
    val diaRevisionSemanal: Int?,
    val horaRecordatorioCierreDia: String?,
    val horaRecordatorioFranjaManana: String?,
    val horaRecordatorioFranjaTarde: String?,
    val horaRecordatorioFranjaNoche: String?,
    val bottomBarPosicion2: String?,
    val bottomBarPosicion3: String?,
    val bottomBarPosicion4: String?,
    val duracionMaximaAlarmaMin: Int?,
)

/** Código para "Compartir seguimiento" con tiempo de vida corto — igual que
 * `CodigoInvitacionEspacio` de Familia, pero para una Actividad puntual (Hábito/Tarea/Rutina/
 * Medicamento/Cita). Escanearlo acompaña de inmediato, sin un paso de "aceptar" aparte — ver
 * `Plan/08-decisiones-tecnicas.md`. */
data class CodigoCompartirActividad(
    val codigoId: String,
    val actividadId: String,
    val tipoActividad: TipoActividad,
    val nombreActividad: String,
    val permiso: PermisoCompartir,
    val deUsuarioId: String,
    val deFirebaseUid: String,
    val deNombre: String,
    val expiraEn: Long,
)

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

    /** Trae el perfil guardado en la nube para esta cuenta — null si nunca se subió nada
     * (cuenta recién vinculada por primera vez en cualquier dispositivo). */
    suspend fun restaurarPerfil(firebaseUid: String): PerfilRemoto?
    suspend fun subirSolicitud(solicitud: SolicitudCompartir)
    suspend fun eliminarSolicitud(solicitudId: String)
    suspend fun subirConexion(conexion: Conexion)

    /** Escucha en vivo las solicitudes dirigidas a mi correo o que yo envié, para reflejar en
     * este dispositivo lo que pase del otro lado (alguien acepta/rechaza, o me llega una
     * nueva). Vacío mientras [miCorreo] esté en blanco (cuenta sin vincular todavía). */
    fun escucharSolicitudes(miUsuarioId: String, miCorreo: String): Flow<List<SolicitudCompartir>>

    /** Genera un código de "Compartir seguimiento" de corta duración, para mostrar como QR. */
    suspend fun generarCodigoCompartir(
        actividadId: String,
        tipoActividad: TipoActividad,
        nombreActividad: String,
        permiso: PermisoCompartir,
        deUsuarioId: String,
        deNombre: String,
    ): CodigoCompartirActividad

    /** Reclama un código escaneado — si sigue vigente y nadie más lo reclamó, marca quién lo
     * reclamó (transacción, mismo patrón que `EspacioSyncRepository.reclamarCodigoInvitacion`)
     * y devuelve sus datos para poder crear la solicitud ya aceptada. */
    suspend fun reclamarCodigoCompartir(codigoId: String, miNombre: String, miCorreo: String): CodigoCompartirActividad?

    /** Escucha en vivo un código que yo generé — mientras el diálogo del QR sigue abierto, para
     * mostrar la confirmación apenas la otra persona lo escanea. Emite el nombre de quien lo
     * reclamó, o null mientras nadie lo haya hecho todavía. */
    fun escucharReclamoDeCodigoCompartir(codigoId: String): Flow<String?>

    /** No-op si la cuenta todavía no está vinculada. */
    suspend fun subirAjustes(ajustes: AjustesRemotos)

    /** null si nunca se subió nada (cuenta recién vinculada, o vinculada pero nunca abrió
     * Ajustes en ningún dispositivo). */
    suspend fun restaurarAjustes(firebaseUid: String): AjustesRemotos?

    /** "Recordarle" — quien acompaña con permiso `PUEDE_VER_Y_RECORDAR` le pide a quien comparte
     * que revise una actividad puntual. */
    suspend fun solicitarRecordatorio(actividadId: String, nombreActividad: String, deNombre: String, paraFirebaseUid: String)

    /** En vivo mientras se escuche — pensado para correr mientras la app esté abierta (ver
     * `TopBarStatsViewModel`), no es un listener permanente en segundo plano. */
    fun escucharRecordatoriosSolicitados(miFirebaseUid: String): Flow<List<RecordatorioSolicitado>>

    /** Se borra apenas se muestra, para no repetir el mismo aviso la próxima vez que se
     * reconecte el listener. */
    suspend fun eliminarRecordatorioSolicitado(recordatorioId: String)
}
