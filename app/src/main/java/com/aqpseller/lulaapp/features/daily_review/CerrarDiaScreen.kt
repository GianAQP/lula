package com.aqpseller.lulaapp.features.daily_review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.emojiHitoRacha
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight

@Composable
fun CerrarDiaScreen(
    onVolverAHoy: () -> Unit,
    onVerHistorial: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CerrarDiaViewModel = hiltViewModel(),
) {
    var queLogre by remember { mutableStateOf("") }
    var queCosto by remember { mutableStateOf("") }
    var queAjusto by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    // Se dispara una sola vez, cuando termina de cargar — si hoy ya se había cerrado antes, esto
    // trae las respuestas existentes en vez de dejar los campos en blanco (ver ViewModel).
    LaunchedEffect(uiState.cargando) {
        if (!uiState.cargando) {
            queLogre = uiState.queLogreInicial.orEmpty()
            queCosto = uiState.queCostoInicial.orEmpty()
            queAjusto = uiState.queAjustoInicial.orEmpty()
        }
    }

    // Un hito de racha (7/21/30/60...) se celebra en grande, ocupando toda la pantalla, en vez
    // de la vista chica normal de "día cerrado" — a pedido del usuario, para que se sienta como
    // un momento aparte, no una línea más de texto. Ver `Plan/08-decisiones-tecnicas.md`.
    val hito = uiState.hitoAlcanzado
    if (uiState.cerrado && hito != null) {
        CelebracionHitoRacha(hito = hito, mensaje = uiState.mensajeCierre, onSeguir = onVolverAHoy, modifier = modifier)
        return
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        if (uiState.cerrado) {
            Text(text = uiState.mensajeCierre, style = MaterialTheme.typography.titleMedium)
            StatPill(
                emoji = "🔥",
                valor = "Racha: ${uiState.rachaFinal} días",
                colorContenedor = LulaRachaContainerLight,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = onVolverAHoy, modifier = Modifier.padding(end = 8.dp)) {
                    Text("Volver a Hoy")
                }
                OutlinedButton(onClick = onVerHistorial) {
                    Text("Ver mi historial")
                }
            }
            return
        }

        val fechaTexto = uiState.fecha?.let { DateTimeUtils.formatearFechaLarga(it) }
        Text(
            text = when {
                uiState.esHoy && uiState.yaExistiaRegistro -> "Actualiza el cierre de hoy"
                uiState.esHoy -> "Cerremos tu día"
                uiState.yaExistiaRegistro -> "Actualiza el cierre del $fechaTexto"
                else -> "Cerremos el $fechaTexto"
            },
            style = MaterialTheme.typography.titleLarge,
        )

        if (uiState.esHoy) {
            Text(
                text = "Actividades: ${uiState.actividadesCompletadas} de ${uiState.actividadesTotales}",
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            // Un día anterior no se puede recalcular en vivo de forma confiable — se escribe a
            // mano, con lo ya guardado como punto de partida si ese día ya se había cerrado.
            Text(
                text = "Lula no puede recalcular ese día solo — escribe cuántas completaste.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = uiState.actividadesCompletadas.toString(),
                    onValueChange = { nuevo -> nuevo.toIntOrNull()?.let { viewModel.actualizarActividadesCompletadas(it) } },
                    label = { Text("Completadas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = uiState.actividadesTotales.toString(),
                    onValueChange = { nuevo -> nuevo.toIntOrNull()?.let { viewModel.actualizarActividadesTotales(it) } },
                    label = { Text("Totales") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        DictationTextField(
            value = queLogre,
            onValueChange = { queLogre = it },
            label = "¿Qué logré hoy? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = queCosto,
            onValueChange = { queCosto = it },
            label = "¿Qué costó más? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )
        DictationTextField(
            value = queAjusto,
            onValueChange = { queAjusto = it },
            label = "¿Qué ajusto para mañana? (opcional)",
            singleLine = false,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            text = "Puntuación del día: ${uiState.actividadesCompletadas} puntos",
            modifier = Modifier.padding(top = 16.dp),
        )

        Button(
            onClick = {
                viewModel.cerrarDia(
                    queLogre.ifBlank { null },
                    queCosto.ifBlank { null },
                    queAjusto.ifBlank { null },
                )
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (uiState.yaExistiaRegistro) "Guardar cambios" else "Guardar y cerrar")
        }
    }
}

/**
 * Celebración de hito de racha (7/21/30/60...) — pantalla completa a propósito, no una tarjeta
 * chica, para que se sienta como un momento aparte de cerrar el día. La "cara" es una plantita
 * que crece (🌱→🌿→🌳, `emojiHitoRacha`) en vez de un ícono nuevo — mismo símbolo que ya usa
 * Lula para "hábitos/crecimiento", y deja listo el camino para más adelante reemplazarla por un
 * personaje propio. El botón queda siempre igual (no rota) para que sea un llamado a la acción
 * reconocible; el mensaje de arriba sí rota entre varios (ver `MensajesRacha.kt`). A pedido del
 * usuario. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@Composable
private fun CelebracionHitoRacha(
    hito: Int,
    mensaje: String,
    onSeguir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emojiHitoRacha(hito), fontSize = 96.sp)
        Text(
            text = "$hito días",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = mensaje,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(onClick = onSeguir, modifier = Modifier.padding(top = 32.dp)) {
            Text("Voy a seguir 🌱")
        }
    }
}
