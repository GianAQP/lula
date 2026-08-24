package com.aqpseller.lulaapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.notifications.AlarmaSonidoService
import com.aqpseller.lulaapp.features.onboarding.OnboardingScreen
import com.aqpseller.lulaapp.navigation.AppViewModel
import com.aqpseller.lulaapp.navigation.LulaNavHost
import com.aqpseller.lulaapp.ui.theme.LulaAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * `FragmentActivity` (no `ComponentActivity`) porque `BiometricPrompt` de la Zona Privada lo
 * requiere — ver `core/security/BiometricAuthenticator.kt`.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var destinoInicial by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        destinoInicial = intent?.getStringExtra(EXTRA_DESTINO)
        mostrarSobreBloqueoSiCorresponde(intent)
        detenerAlarmaSiCorresponde(intent)
        setContent {
            LulaAppTheme {
                val appViewModel: AppViewModel = hiltViewModel()
                val isReady by appViewModel.isReady.collectAsState()
                val onboardingCompletado by appViewModel.onboardingCompletado.collectAsState()

                SolicitarPermisoNotificaciones()

                when {
                    !isReady || onboardingCompletado == null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    onboardingCompletado == false -> {
                        OnboardingScreen(onTerminado = {})
                    }
                    else -> {
                        LulaNavHost(
                            destinoInicial = destinoInicial,
                            onDestinoConsumido = { destinoInicial = null },
                        )
                    }
                }
            }
        }
    }

    /** La Activity ya puede estar abierta (app en segundo plano) — Android no vuelve a llamar onCreate. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destinoInicial = intent.getStringExtra(EXTRA_DESTINO)
        mostrarSobreBloqueoSiCorresponde(intent)
        detenerAlarmaSiCorresponde(intent)
    }

    /**
     * Solo para el nivel de recordatorio "Alarma": sin esto, el `fullScreenIntent` puede abrir
     * la Activity detrás de la pantalla bloqueada (pantalla oscura, sin nada visible) en vez
     * de mostrarla — a pedido del usuario, que reportó justo ese síntoma.
     */
    private fun mostrarSobreBloqueoSiCorresponde(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_MOSTRAR_SOBRE_BLOQUEO, false) != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
    }

    /**
     * Tocar la notificación de una Alarma también debe cortar el loop de sonido — sin esto,
     * `AlarmaSonidoService` seguiría sonando de fondo aunque la persona ya haya abierto la app.
     */
    private fun detenerAlarmaSiCorresponde(intent: Intent?) {
        val claveNotificacion = intent?.getIntExtra(EXTRA_DETENER_ALARMA, 0) ?: 0
        if (claveNotificacion == 0) return
        startService(AlarmaSonidoService.intentDetener(this, claveNotificacion))
    }

    companion object {
        /** Ruta de `LulaDestinations` a la que navegar al tocar una notificación — ver `RecordatorioReceiver`. */
        const val EXTRA_DESTINO = "destino"

        /** Solo lo pone `RecordatorioReceiver` para el nivel Alarma. */
        const val EXTRA_MOSTRAR_SOBRE_BLOQUEO = "mostrarSobreBloqueo"

        /** Clave de la notificación de Alarma a detener — solo lo pone `RecordatorioReceiver` para ese nivel. */
        const val EXTRA_DETENER_ALARMA = "detenerAlarma"
    }
}

/** Solo hace falta desde Android 13 (API 33) — antes las notificaciones no piden permiso. */
@Composable
private fun SolicitarPermisoNotificaciones() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val yaConcedido = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!yaConcedido) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
