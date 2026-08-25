package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import com.aqpseller.lulaapp.domain.model.RolEnEspacio
import com.aqpseller.lulaapp.domain.model.TipoEspacio
import kotlinx.coroutines.flow.Flow

interface EspacioRepository {
    suspend fun contarEspacios(): Int
    suspend fun crearEspacioPersonal(espacio: Espacio, miembro: EspacioMiembro)
    suspend fun obtenerEspacioPersonal(usuarioId: String): Espacio?

    /** Crea un espacio de cualquier tipo (Familia/Equipo) — `crearEspacioPersonal` sigue existiendo
     * para el usuario semilla, este es el genérico usado desde Fase 1.5 en adelante. */
    suspend fun crearEspacio(espacio: Espacio, miembro: EspacioMiembro)

    /** Espacios a los que pertenece el usuario (Personal siempre incluido) — para el selector de espacio. */
    fun observarEspaciosDeUsuario(usuarioId: String): Flow<List<Espacio>>

    /** Null si el espacio no existe o el usuario no es miembro — nunca confiar en un id guardado sin validar. */
    suspend fun obtenerEspacioSiEsMiembro(espacioId: String, usuarioId: String): Espacio?

    fun observarMiembros(espacioId: String): Flow<List<EspacioMiembro>>

    /** Al aceptar una invitación real a un Espacio Familia — agrega a [usuarioId] como miembro
     * con [rol]. [nombre]/[firebaseUid] null dejan el valor ya guardado sin tocar (nunca lo
     * borran) — así un respaldo/sync sin ese dato todavía no pisa uno que ya se sabía. Ver
     * `Plan/12-firebase-auth-y-sync.md`. */
    suspend fun agregarMiembro(espacioId: String, usuarioId: String, rol: RolEnEspacio, nombre: String? = null, firebaseUid: String? = null)

    /** Quita a [usuarioId] del espacio — para "salir" (yo mismo) o que un admin quite a otro.
     * La validación de quién puede hacerlo vive en el caso de uso, no acá. */
    suspend fun eliminarMiembro(espacioId: String, usuarioId: String, ejecutadoPor: String)

    /** Si acepto una invitación a un Espacio que vive en OTRO dispositivo (el de quien me
     * invitó), ese Espacio no existe en mi base local — y `agregarMiembro` fallaría por la FK
     * hacia `espacio`. Crea un mirror mínimo solo si no existe ya (nunca pisa uno real). Ver
     * `Plan/12-firebase-auth-y-sync.md`. */
    suspend fun asegurarEspacioMinimo(espacioId: String, nombre: String, creadoPor: String, tipo: TipoEspacio)

    suspend fun renombrarEspacio(espacioId: String, nuevoNombre: String, usuarioId: String)

    /** Borra el espacio Y todo lo creado dentro de él (actividades, metas, listas, movimientos
     * financieros, retos familiares) — para siempre, sin forma de deshacerlo. */
    suspend fun eliminarEspacio(espacioId: String, usuarioId: String)

    suspend fun contarAreasDeVida(): Int
    suspend fun sembrarAreasDeVida(areas: List<AreaDeVida>)
    fun observarAreasDeVidaActivas(): Flow<List<AreaDeVida>>
}
