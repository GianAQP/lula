package com.aqpseller.lulaapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombreCompleto: String,
    val nombrePreferido: String,
    val correo: String?,
    val metodoLogin: String,
    val privacidadAceptadaEn: Long?,
    val modoDefectoAsistente: String?,
    val horaDesayuno: String? = null,
    val horaAlmuerzo: String? = null,
    val horaCena: String? = null,
    val confirmoMayorDe13: Boolean = false,
    val terminosAceptadosEn: Long? = null,
    val consentimientoDatosSaludEn: Long? = null,
    /** uid de Firebase Auth una vez que la cuenta se "reclama" con Google/correo mágico —
     * null mientras siga siendo solo el usuario semilla local. Ver
     * `Plan/12-firebase-auth-y-sync.md`. */
    val firebaseUid: String? = null,
    /** null = todavía no pasó por el registro/preguntas iniciales — gatilla mostrar
     * `OnboardingScreen` en vez de entrar directo a Hoy. Ver `Plan/06-onboarding.md`. */
    val onboardingCompletadoEn: Long? = null,
    /** Respuestas del onboarding — JSON vía `encodeStringList`/lista simple. Ver `Plan/06-onboarding.md`. */
    val queMejorarJson: String = "[]",
    val comoEmpezar: String? = null,
    val momentoDelDiaPreferido: String? = null,
    val porQueEmpezar: String? = null,
)
