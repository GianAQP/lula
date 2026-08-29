package com.aqpseller.lulaapp.domain.usecase.espacio

import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioSyncRepository
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Escucha en vivo Firestore mientras se llame (pensado para correr mientras el Espacio Familia
 * activo lo sea — se cancela solo al cambiar de espacio o cerrar la app) y refleja cada cambio
 * remoto en Room: miembros, Tareas y Retos familiares (con quién ya los cumplió hoy). Room sigue
 * siendo la única fuente de verdad para la UI — Firestore es solo transporte. Ver
 * `Plan/12-firebase-auth-y-sync.md`, paso 5.
 */
class SincronizarEspacioFamiliaUseCase @Inject constructor(
    private val espacioSyncRepository: EspacioSyncRepository,
    private val espacioRepository: EspacioRepository,
    private val actividadRepository: ActividadRepository,
    private val retoFamiliarRepository: RetoFamiliarRepository,
    private val usuarioRepository: UsuarioRepository,
) {
    suspend operator fun invoke(espacioId: String, miUsuarioId: String) = coroutineScope {
        respaldarMiPresenciaRemota(espacioId, miUsuarioId)

        launch {
            espacioSyncRepository.escucharMiembros(espacioId).collect { miembros ->
                miembros.forEach { espacioRepository.agregarMiembro(it.espacioId, it.usuarioId, it.rol, it.nombre, it.firebaseUid) }
            }
        }
        launch {
            espacioSyncRepository.escucharTareas(espacioId).collect { tareas ->
                tareas.forEach { (actividad, detalle) -> actividadRepository.mergeTareaRemota(actividad, detalle) }
            }
        }
        launch {
            espacioSyncRepository.escucharRetos(espacioId).collect { retos ->
                retos.forEach { retoFamiliarRepository.mergeRemoto(it) }
            }
        }
        launch {
            espacioSyncRepository.escucharRegistrosReto(espacioId).collect { registros ->
                registros.forEach { retoFamiliarRepository.mergeRegistroRemoto(it.retoId, it.usuarioId, it.fecha, it.estado) }
            }
        }
    }

    /**
     * Si este Espacio se creó antes de que existiera este sync (o el push original falló por
     * estar sin conexión), las reglas de seguridad rechazan todo — `esMiembro()` da falso
     * porque nunca se subió mi membresía. Se asegura acá, antes de escuchar, para que un
     * Espacio Familia viejo también empiece a funcionar sin tener que recrearlo.
     */
    private suspend fun respaldarMiPresenciaRemota(espacioId: String, miUsuarioId: String) {
        runCatching {
            val espacio = espacioRepository.obtenerEspacioSiEsMiembro(espacioId, miUsuarioId) ?: return
            val miembrosLocales = espacioRepository.observarMiembros(espacioId).first()
            val encontrado = miembrosLocales.find { it.usuarioId == miUsuarioId }
            // Si mi fila de miembro es de antes de que existiera `nombre` (o nunca se guardó),
            // se autorrepara acá mismo con mi nombre actual — mismo criterio que el resto de
            // este método (arreglar espacios/membresías viejas de paso, sin migración especial).
            val miNombre = encontrado?.nombre ?: usuarioRepository.observarUsuario().first()?.nombrePreferido
            // Bug real encontrado 2026-08-27: acá abajo el rol por defecto para "encontrado ==
            // null" era MIEMBRO a secas — si este método corría en una carrera de tiempos justo
            // después de `CrearEspacioFamiliaUseCase` (antes de que mi propia fila local de
            // ADMIN terminara de guardarse), pisaba mi membresía real con MIEMBRO, tanto acá
            // como en Firestore — y de ahí el listener de `escucharMiembros` bajaba también la
            // copia local ya buena a MIEMBRO. Nunca asumir MIEMBRO por defecto para quien de
            // verdad creó el espacio: si no hay nadie con ADMIN todavía y yo soy `creadoPor`,
            // me quedo como ADMIN — esto también autorrepara un espacio que ya haya quedado sin
            // ningún admin por este mismo bug. Ver `Plan/08-decisiones-tecnicas.md`.
            val soyCreador = espacio.creadoPor == miUsuarioId
            val nadieEsAdmin = miembrosLocales.none { it.rol == RolEnEspacio.ADMIN }
            val miRol = when {
                encontrado?.rol == RolEnEspacio.ADMIN -> RolEnEspacio.ADMIN
                soyCreador && (encontrado == null || nadieEsAdmin) -> RolEnEspacio.ADMIN
                else -> encontrado?.rol ?: RolEnEspacio.MIEMBRO
            }
            val miMiembro = (encontrado ?: EspacioMiembro(espacioId = espacioId, usuarioId = miUsuarioId, rol = miRol))
                .copy(nombre = miNombre, rol = miRol)
            espacioSyncRepository.subirEspacio(espacio)
            espacioSyncRepository.subirMiembro(espacioId, miMiembro)
            espacioSyncRepository.subirPunteroMiEspacio(espacioId)
            if (encontrado?.nombre == null || encontrado.rol != miRol) {
                espacioRepository.agregarMiembro(espacioId, miUsuarioId, miRol, miNombre)
            }
        }
    }
}
