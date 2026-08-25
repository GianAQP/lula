package com.aqpseller.lulaapp.domain.usecase.usuario

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import com.aqpseller.lulaapp.domain.usecase.registrodiario.ObtenerProgresoDeHoyUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val UN_DIA_MS = 24L * 60 * 60 * 1000
private const val DIAS_PARA_PLANTITA = 30
private const val DIAS_PARA_PODER_FLORECER = 60

private enum class EstadoIconoApp(val alias: String) {
    SEMILLA(".LauncherSemilla"),
    PLANTITA(".LauncherPlantita"),
    FLOR(".LauncherFlor"),
}

/**
 * El ícono de la app evoluciona solo con el tiempo — semilla los primeros 30 días, plantita de
 * ahí hasta el día 60, y desde el día 60 en adelante florece mientras haya racha activa (vuelve
 * a plantita sin flor si se corta, nunca retrocede a semilla). Pedido explícito del usuario
 * (2026-08-23), ver `Plan/08-decisiones-tecnicas.md`.
 *
 * El ícono real vive en 3 `activity-alias` del manifest — cambiar de estado es habilitar uno y
 * deshabilitar los otros dos con `PackageManager`, no reinicia ni mata el proceso en curso. Se
 * llama best-effort en cada apertura de la app (`AppViewModel`), igual que el resto de checks
 * periódicos de esta clase.
 */
class ActualizarIconoAppUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usuarioRepository: UsuarioRepository,
    private val espacioRepository: EspacioRepository,
    private val obtenerProgresoDeHoyUseCase: ObtenerProgresoDeHoyUseCase,
) {
    suspend operator fun invoke() {
        val usuario = usuarioRepository.observarUsuario().first() ?: return
        val inicio = usuario.onboardingCompletadoEn ?: return
        val dias = (System.currentTimeMillis() - inicio) / UN_DIA_MS

        val estado = when {
            dias < DIAS_PARA_PLANTITA -> EstadoIconoApp.SEMILLA
            dias < DIAS_PARA_PODER_FLORECER -> EstadoIconoApp.PLANTITA
            else -> {
                val espacioPersonal = espacioRepository.obtenerEspacioPersonal(usuario.id)
                val racha = espacioPersonal?.let { obtenerProgresoDeHoyUseCase.calcularRachaActual(it.id) } ?: 0
                if (racha > 0) EstadoIconoApp.FLOR else EstadoIconoApp.PLANTITA
            }
        }

        aplicarEstado(estado)
    }

    private fun aplicarEstado(estado: EstadoIconoApp) {
        val packageManager = context.packageManager
        EstadoIconoApp.entries.forEach { candidato ->
            val componente = ComponentName(context, "${context.packageName}${candidato.alias}")
            val deseado = if (candidato == estado) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            // Evita escribir si ya está así — no hace falta, pero PackageManager tolera
            // llamadas redundantes sin efecto secundario visible (no reinicia la app).
            if (packageManager.getComponentEnabledSetting(componente) != deseado) {
                packageManager.setComponentEnabledSetting(componente, deseado, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
