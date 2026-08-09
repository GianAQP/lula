package com.aqpseller.lulaapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AjustesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AjustesRepository {

    private val sonidoCheckKey = booleanPreferencesKey("sonido_check_habilitado")
    private val diaRevisionSemanalKey = intPreferencesKey("dia_revision_semanal")
    private val horaRecordatorioCierreDiaKey = stringPreferencesKey("hora_recordatorio_cierre_dia")
    private val bottomBarPosicion2Key = stringPreferencesKey("bottom_bar_posicion_2")
    private val bottomBarPosicion3Key = stringPreferencesKey("bottom_bar_posicion_3")
    private val bottomBarPosicion4Key = stringPreferencesKey("bottom_bar_posicion_4")

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
    }

    override fun observarDiaRevisionSemanal() =
        context.ajustesDataStore.data.map { it[diaRevisionSemanalKey] ?: 7 }

    override suspend fun setDiaRevisionSemanal(diaIso: Int) {
        context.ajustesDataStore.edit { it[diaRevisionSemanalKey] = diaIso }
    }

    override fun observarHoraRecordatorioCierreDia() =
        context.ajustesDataStore.data.map { it[horaRecordatorioCierreDiaKey] }

    override suspend fun setHoraRecordatorioCierreDia(hora: String?) {
        context.ajustesDataStore.edit {
            if (hora != null) it[horaRecordatorioCierreDiaKey] = hora else it.remove(horaRecordatorioCierreDiaKey)
        }
    }

    // Valores por defecto = "Hoy | 🎙️ Asistente | + | ✅ Hábitos | 💰 Finanzas" de `02-pantallas.md`.
    override fun observarBottomBarPosicion2() =
        context.ajustesDataStore.data.map { it[bottomBarPosicion2Key] ?: "asistente" }

    override suspend fun setBottomBarPosicion2(opcionId: String) {
        context.ajustesDataStore.edit { it[bottomBarPosicion2Key] = opcionId }
    }

    override fun observarBottomBarPosicion3() =
        context.ajustesDataStore.data.map { it[bottomBarPosicion3Key] ?: "habitos" }

    override suspend fun setBottomBarPosicion3(opcionId: String) {
        context.ajustesDataStore.edit { it[bottomBarPosicion3Key] = opcionId }
    }

    override fun observarBottomBarPosicion4() =
        context.ajustesDataStore.data.map { it[bottomBarPosicion4Key] ?: "finanzas" }

    override suspend fun setBottomBarPosicion4(opcionId: String) {
        context.ajustesDataStore.edit { it[bottomBarPosicion4Key] = opcionId }
    }

    override suspend fun obtenerEspacioActivoId(): String? = espacioActivoId.value

    override fun observarEspacioActivoId(): Flow<String?> = espacioActivoId

    override suspend fun setEspacioActivoId(espacioId: String?) {
        this.espacioActivoId.value = espacioId
    }
}
