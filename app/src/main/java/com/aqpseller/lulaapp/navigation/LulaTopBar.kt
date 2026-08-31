package com.aqpseller.lulaapp.navigation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.core.utils.escanearQr
import com.aqpseller.lulaapp.ui.theme.LulaFamiliaContainerDark
import com.aqpseller.lulaapp.ui.theme.LulaFamiliaContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaFinanzasContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight
import com.aqpseller.lulaapp.ui.theme.lulaContainerColor
import com.aqpseller.lulaapp.ui.theme.lulaContentColorSobreContainer
import kotlinx.coroutines.launch

/**
 * Fila fija en todas las pantallas (mismo nivel que el menú "⋮"): racha 🔥, gastos de hoy 💰 —
 * antes solo vivían dentro de Hoy, dejando este espacio vacío en el resto — y un aviso de
 * invitación pendiente si llegara alguna (hoy siempre vacío en un solo dispositivo, ver
 * `Plan/08-decisiones-tecnicas.md`, pero queda conectado para activarse solo).
 */
@Composable
fun LulaTopBar(
    currentRoute: String?,
    onAbrirAjustes: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onAbrirFamilia: () -> Unit,
    onAbrirCirculoDeCuidado: () -> Unit,
    onAbrirNotificaciones: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerFinanzas: () -> Unit,
    onAbrirDiario: () -> Unit,
    viewModel: TopBarStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarExplicacionRacha by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(currentRoute) { viewModel.refrescar() }
    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.mensajeMostrado()
        }
    }
    LaunchedEffect(uiState.correoParaCopiar) {
        uiState.correoParaCopiar?.let {
            clipboardManager.setText(AnnotatedString(it))
            viewModel.correoCopiado()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        // Banda de color propio (nunca usado para otra cosa) para que se note en CUALQUIER
        // pantalla que el espacio activo no es Personal — antes solo se avisaba dentro de Hoy,
        // y el usuario se "perdía" al cambiar de pantalla pensando que sus datos habían
        // desaparecido. Ver feedback del usuario, 2026-07-30, `08-decisiones-tecnicas.md`.
        uiState.nombreEspacioActivo?.let { nombreEspacio ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(lulaContainerColor(LulaFamiliaContainerLight, LulaFamiliaContainerDark))
                    .clickable(onClick = onAbrirFamilia)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "👨‍👩‍👧 Estás en $nombreEspacio",
                    style = MaterialTheme.typography.labelLarge,
                    color = lulaContentColorSobreContainer(),
                    modifier = Modifier.weight(1f),
                )
                Text(text = "Cambiar", style = MaterialTheme.typography.labelLarge, color = lulaContentColorSobreContainer())
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // La app usa `enableEdgeToEdge()` — el contenido de `Scaffold.topBar` es
                // responsable de su propio inset de la barra de estado (`TopAppBar` lo maneja
                // solo; esta fila no lo hacía, así que el ícono "⋮" quedaba dibujado detrás del
                // reloj/batería, invisible en toda la app). Ese inset ahora vive en la `Column`
                // que envuelve todo, para que la banda de espacio activo también respete la
                // barra de estado.
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatPill(
                emoji = "🔥",
                valor = "${uiState.racha} días",
                colorContenedor = LulaRachaContainerLight,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { mostrarExplicacionRacha = true },
            )
            StatPill(
                emoji = "💰",
                // Sin el "-" un "20" ahí no se entendía como gasto (podía leerse como cualquier
                // número) — a pedido del usuario. Solo cuando hay algo que restar; en 0 no tiene
                // sentido un "-0". Ver `Plan/08-decisiones-tecnicas.md`.
                valor = if (uiState.gastosHoyTotal > 0) {
                    "-S/ ${"%.0f".format(uiState.gastosHoyTotal)}"
                } else {
                    "S/ ${"%.0f".format(uiState.gastosHoyTotal)}"
                },
                colorContenedor = LulaFinanzasContainerLight,
                modifier = Modifier.clickable(onClick = onVerFinanzas),
            )

            Box(modifier = Modifier.weight(1f))

            // Siempre visible (antes solo aparecía si había algo pendiente) — abre el historial
            // real de notificaciones, no "Mi círculo de cuidado" directo (que sigue aparte, en
            // el menú "⋮", como pantalla de gestión). Ver `Plan/08-decisiones-tecnicas.md`.
            IconButton(onClick = onAbrirNotificaciones) {
                BadgedBox(
                    badge = {
                        if (uiState.notificacionesNoLeidas > 0) {
                            Badge { Text("${uiState.notificacionesNoLeidas}") }
                        }
                    },
                ) {
                    // Silueta neutra sin nada pendiente; se "enciende" en amarillo apenas hay
                    // algo por leer — antes el emoji 🔔 se veía igual de amarillo siempre,
                    // sin distinguir a simple vista si había algo nuevo. Ver
                    // `Plan/08-decisiones-tecnicas.md`.
                    if (uiState.notificacionesNoLeidas > 0) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = Color(0xFFFFC107))
                    } else {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
                    }
                }
            }

            // Un solo botón de escanear, visible en toda la app — detecta solo qué tipo de
            // código de Lula es (Lista, contacto para conectar) en vez de tener uno distinto
            // enterrado en cada pantalla. A pedido del usuario, ver `08-decisiones-tecnicas.md`.
            // Ícono real (no emoji) — un emoji de QR no se entendía, ver misma nota.
            IconButton(onClick = { scope.launch { escanearQr(context)?.let { viewModel.escanear(it) } } }) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear código")
            }

            // El ancla de `DropdownMenu` es el layout que lo contiene directamente — envolver
            // el ícono y el menú juntos evita que el menú se desplace lejos del botón.
            Box {
                IconButton(onClick = { mostrarMenu = true }) {
                    Text("⋮", style = MaterialTheme.typography.titleLarge)
                }
                DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("🧑 Mi perfil") },
                        onClick = { mostrarMenu = false; onAbrirPerfil() },
                    )
                    DropdownMenuItem(
                        text = { Text("👥 Mi círculo de cuidado") },
                        onClick = { mostrarMenu = false; onAbrirCirculoDeCuidado() },
                    )
                    DropdownMenuItem(
                        text = { Text("📓 Diario") },
                        onClick = { mostrarMenu = false; onAbrirDiario() },
                    )
                    DropdownMenuItem(
                        text = { Text("👨‍👩‍👧 Familia / Espacios") },
                        onClick = { mostrarMenu = false; onAbrirFamilia() },
                    )
                    DropdownMenuItem(
                        text = { Text("⚙️ Ajustes") },
                        onClick = { mostrarMenu = false; onAbrirAjustes() },
                    )
                }
            }
        }
    }

    if (mostrarExplicacionRacha) {
        AlertDialog(
            onDismissRequest = { mostrarExplicacionRacha = false },
            confirmButton = {
                TextButton(onClick = { mostrarExplicacionRacha = false; onVerHistorial() }) { Text("Ver historial") }
            },
            dismissButton = { TextButton(onClick = { mostrarExplicacionRacha = false }) { Text("Cerrar") } },
            title = { Text("🔥 Tu racha") },
            text = {
                Text(
                    "Sube cada vez que cierras tu día (con al menos una actividad cumplida). " +
                        "Si te saltas un día no baja — se queda igual hasta que vuelvas a cerrar. " +
                        "¿Se te pasó un día? Puedes cerrarlo desde el calendario.",
                )
            },
        )
    }
}
