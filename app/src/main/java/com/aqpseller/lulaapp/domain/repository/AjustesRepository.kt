package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import kotlinx.coroutines.flow.Flow

interface AjustesRepository {
    fun observarSonidoCheckHabilitado(): Flow<Boolean>
    suspend fun setSonidoCheckHabilitado(habilitado: Boolean)

    /** Día ISO (1=lunes..7=domingo) en que se activa Revisión semanal. Por defecto 7 (domingo). */
    fun observarDiaRevisionSemanal(): Flow<Int>
    suspend fun setDiaRevisionSemanal(diaIso: Int)

    /** Hora "HH:mm" del recordatorio diario para cerrar el día — null = desactivado (por
     * defecto). Ver `Plan/08-decisiones-tecnicas.md`. */
    fun observarHoraRecordatorioCierreDia(): Flow<String?>
    suspend fun setHoraRecordatorioCierreDia(hora: String?)

    /** Hora "HH:mm" de un recordatorio genérico "revisa Lula" por franja del día — null =
     * desactivado (por defecto en las 3). Independiente del recordatorio de cierre del día:
     * este es para no perderse de revisar/marcar nada durante el día, no solo al final. Ver
     * `Plan/08-decisiones-tecnicas.md`. */
    fun observarHoraRecordatorioFranja(momento: MomentoDelDia): Flow<String?>
    suspend fun setHoraRecordatorioFranja(momento: MomentoDelDia, hora: String?)

    /**
     * Qué va en cada posición configurable de la barra inferior (`Plan/02-pantallas.md`:
     * "[ Hoy 🏠 ] [Posición 2] [ + ] [Posición 3] [Posición 4]"). Cada valor es el `id` de un
     * `OpcionBottomBar` (`navigation/OpcionBottomBar.kt`) — este repositorio guarda el string
     * tal cual, sin conocer las rutas de navegación (esas viven en la capa de navegación, no
     * en dominio).
     */
    fun observarBottomBarPosicion2(): Flow<String>
    suspend fun setBottomBarPosicion2(opcionId: String)
    fun observarBottomBarPosicion3(): Flow<String>
    suspend fun setBottomBarPosicion3(opcionId: String)
    fun observarBottomBarPosicion4(): Flow<String>
    suspend fun setBottomBarPosicion4(opcionId: String)

    /** Id del `Espacio` activo (null = Personal, el de por defecto) — ver "selector de espacio",
     * `Plan/02-pantallas.md`. `ObtenerSesionActualUseCase` valida que el usuario siga siendo
     * miembro antes de usarlo, nunca confía en este valor a ciegas. */
    suspend fun obtenerEspacioActivoId(): String?
    fun observarEspacioActivoId(): Flow<String?>
    suspend fun setEspacioActivoId(espacioId: String?)
}
