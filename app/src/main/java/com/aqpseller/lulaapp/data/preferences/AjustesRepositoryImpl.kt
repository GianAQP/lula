package com.aqpseller.lulaapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.domain.repository.AjustesRemotos
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.CompartirSyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AjustesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val compartirSyncRepository: CompartirSyncRepository,
) : AjustesRepository {

    private val sonidoCheckKey = booleanPreferencesKey("sonido_check_habilitado")
    private val diaRevisionSemanalKey = intPreferencesKey("dia_revision_semanal")
    private val horaRecordatorioCierreDiaKey = stringPreferencesKey("hora_recordatorio_cierre_dia")

    private fun horaRecordatorioFranjaKey(momento: MomentoDelDia) =
        stringPreferencesKey("hora_recordatorio_franja_${momento.name}")
    private val bottomBarPosicion2Key = stringPreferencesKey("bottom_bar_posicion_2")
    private val bottomBarPosicion3Key = stringPreferencesKey("bottom_bar_posicion_3")
    private val bottomBarPosicion4Key = stringPreferencesKey("bottom_bar_posicion_4")
    private val ultimoHitoRachaCelebradoKey = intPreferencesKey("ultimo_hito_racha_celebrado")
    private val duracionMaximaAlarmaMinKey = intPreferencesKey("duracion_maxima_alarma_min")

    /**
     * A propósito NO se guarda en DataStore (a diferencia de todo lo demás en esta clase): el
     * usuario probó cambiarse a Familia, cerró la app y la volvió a abrir esperando ver
     * Personal de nuevo (su espacio de siempre) — encontrarse todavía en Familia lo hizo pensar
     * que sus datos personales se habían borrado. Un `MutableStateFlow` en este singleton dura
     * lo que dura el proceso: sobrevive mientras se navega dentro de la app, pero se resetea
     * solo al volver a abrirla. Ver `Plan/08-decisiones-tecnicas.md`.
     */
    private val espacioActivoId = MutableStateFlow<String?>(null)

    override fun observarSonidoCheckHabilitado() =
        context.ajustesDataStore.data.map { it[sonidoCheckKey] ?: true }

    override suspend fun setSonidoCheckHabilitado(habilitado: Boolean) {
        context.ajustesDataStore.edit { it[sonidoCheckKey] = habilitado }
        sincronizarConNube()
    }

    override fun observarDiaRevisionSemanal() =
        context.ajustesDataStore.data.map { it[diaRevisionSemanalKey] ?: 7 }

    override suspend fun setDiaRevisionSemanal(diaIso: Int) {
        context.ajustesDataStore.edit { it[diaRevisionSemanalKey] = diaIso }
        sincronizarConNube()
    }

    override fun observarHoraRecordatorioCierreDia() =
        context.ajustesDataStore.data.map { it[horaRecordatorioCierreDiaKey] }

    override suspend fun setHoraRecordatorioCierreDia(hora: String?) {
        context.ajustesDataStore.edit {
            if (hora != null) it[horaRecordatorioCierreDiaKey] = hora else it.remove(horaRecordatorioCierreDiaKey)
        }
        sincronizarConNube()
    }

    override fun observarHoraRecordatorioFranja(momento: MomentoDelDia) =
        context.ajustesDataStore.data.map { it[horaRecordatorioFranjaKey(momento)] }

    override suspend fun setHoraRecordatorioFranja(momento: MomentoDelDia, hora: String?) {
        val key = horaRecordatorioFranjaKey(momento)
        context.ajustesDataStore.edit {
            if (hora != null) it[key] = hora else it.remove(key)
        }
        sincronizarConNube()
    }

    // Valores por defecto = "Hoy | 🎙️ Asistente | + | ✅ Hábitos | 💰 Finanzas" de `02-pantallas.md`.
    override fun observarBottomBarPosicion2() =
        context.ajustesDataStore.data.map { it[bottomBarPosicion2Key] ?: "asistente" }

    override suspend fun setBottomBarPosicion2(opcionId: String) {
        context.ajustesDataStore.edit { it[bottomBarPosicion2Key] = opcionId }
        sincronizarConNube()
    }

    override fun observarBottomBarPosicion3() =
        context.ajustesDataStore.data.map { it[bottomBarPosicion3Key] ?: "habitos" }

    override suspend fun setBottomBarPosicion3(opcionId: String) {
        context.ajustesDataStore.edit { it[bottomBarPosicion3Key] = opcionId }
        sincronizarConNube()
    }

    override fun observarBottomBarPosicion4() =
        context.ajustesDataStore.data.map { it[bottomBarPosicion4Key] ?: "finanzas" }

    override suspend fun setBottomBarPosicion4(opcionId: String) {
        context.ajustesDataStore.edit { it[bottomBarPosicion4Key] = opcionId }
        sincronizarConNube()
    }

    override suspend fun obtenerEspacioActivoId(): String? = espacioActivoId.value

    override fun observarEspacioActivoId(): Flow<String?> = espacioActivoId

    override suspend fun setEspacioActivoId(espacioId: String?) {
        this.espacioActivoId.value = espacioId
    }

    override suspend fun obtenerUltimoHitoRachaCelebrado(): Int =
        context.ajustesDataStore.data.first()[ultimoHitoRachaCelebradoKey] ?: 0

    override suspend fun setUltimoHitoRachaCelebrado(valor: Int) {
        context.ajustesDataStore.edit { it[ultimoHitoRachaCelebradoKey] = valor }
    }

    override fun observarDuracionMaximaAlarmaMin(): Flow<Int?> =
        context.ajustesDataStore.data.map { it[duracionMaximaAlarmaMinKey] }

    override suspend fun setDuracionMaximaAlarmaMin(minutos: Int?) {
        context.ajustesDataStore.edit {
            if (minutos != null) it[duracionMaximaAlarmaMinKey] = minutos else it.remove(duracionMaximaAlarmaMinKey)
        }
        sincronizarConNube()
    }

    /** Sube una foto completa de todos los Ajustes sincronizables cada vez que cambia cualquiera
     * — más simple que rastrear qué campo cambió, y el documento entero es chico. No-op si la
     * cuenta no está vinculada (lo resuelve `CompartirSyncRepository.subirAjustes`). A propósito
     * NO incluye `espacioActivoId` (vive solo en memoria, ver el campo de esta clase) ni
     * `ultimoHitoRachaCelebrado` (no es una preferencia real). Ver
     * `Plan/08-decisiones-tecnicas.md`. */
    private suspend fun sincronizarConNube() {
        runCatching {
            compartirSyncRepository.subirAjustes(
                AjustesRemotos(
                    sonidoCheckHabilitado = observarSonidoCheckHabilitado().first(),
                    diaRevisionSemanal = observarDiaRevisionSemanal().first(),
                    horaRecordatorioCierreDia = observarHoraRecordatorioCierreDia().first(),
                    horaRecordatorioFranjaManana = observarHoraRecordatorioFranja(MomentoDelDia.MANANA).first(),
                    horaRecordatorioFranjaTarde = observarHoraRecordatorioFranja(MomentoDelDia.TARDE).first(),
                    horaRecordatorioFranjaNoche = observarHoraRecordatorioFranja(MomentoDelDia.NOCHE).first(),
                    bottomBarPosicion2 = observarBottomBarPosicion2().first(),
                    bottomBarPosicion3 = observarBottomBarPosicion3().first(),
                    bottomBarPosicion4 = observarBottomBarPosicion4().first(),
                    duracionMaximaAlarmaMin = observarDuracionMaximaAlarmaMin().first(),
                ),
            )
        }
    }
}
