package com.aqpseller.lulaapp.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aqpseller.lulaapp.domain.repository.PrivacidadRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject

/**
 * PIN local para la Zona Privada — un candado de UI, no protección criptográfica de los
 * datos (que ya viven cifrados en tránsito cuando haya sync, ver `01-arquitectura.md`). Se
 * guarda solo el hash SHA-256, nunca el PIN en texto plano.
 */
class PrivacidadRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PrivacidadRepository {

    private val pinHashKey = stringPreferencesKey("pin_hash")

    override suspend fun estaConfigurada(): Boolean =
        dataStore.data.map { it[pinHashKey] != null }.first()

    override suspend fun configurarPin(pin: String) {
        dataStore.edit { it[pinHashKey] = hashDe(pin) }
    }

    override suspend fun verificarPin(pin: String): Boolean {
        val hashGuardado = dataStore.data.map { it[pinHashKey] }.first()
        return hashGuardado != null && hashGuardado == hashDe(pin)
    }

    private fun hashDe(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("lula_zona_privada_$pin".toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}
