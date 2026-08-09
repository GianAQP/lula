package com.aqpseller.lulaapp.features.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.security.mostrarPromptBiometrico

@Composable
fun PrivacyGateScreen(
    onDesbloqueado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacyGateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.desbloqueada) { if (uiState.desbloqueada) onDesbloqueado() }

    if (uiState.cargando || uiState.desbloqueada) return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!uiState.configurada) {
            ConfigurarPin(onConfirmar = viewModel::configurarPin)
        } else {
            DesbloquearZonaPrivada(
                biometriaDisponible = uiState.biometriaDisponible,
                pinIncorrecto = uiState.pinIncorrecto,
                onIntentarPin = viewModel::intentarPin,
                onDesbloquearConBiometria = viewModel::desbloquearConBiometria,
            )
        }
    }
}

@Composable
private fun ConfigurarPin(onConfirmar: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var noCoincide by remember { mutableStateOf(false) }

    Text(text = "🔒 Protege tu espacio privado", style = MaterialTheme.typography.titleLarge)
    Text(
        text = "Acá viven tus finanzas y notas privadas. Crea un PIN de 4 a 6 dígitos.",
        modifier = Modifier.padding(top = 8.dp),
    )
    OutlinedTextField(
        value = pin,
        onValueChange = { nuevo -> if (nuevo.length <= 6 && nuevo.all { it.isDigit() }) pin = nuevo },
        label = { Text("PIN") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    )
    OutlinedTextField(
        value = confirmacion,
        onValueChange = { nuevo -> if (nuevo.length <= 6 && nuevo.all { it.isDigit() }) confirmacion = nuevo; noCoincide = false },
        label = { Text("Confirma tu PIN") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
    if (noCoincide) {
        Text(text = "Los PIN no coinciden", modifier = Modifier.padding(top = 4.dp))
    }
    Button(
        onClick = {
            if (pin.length >= 4 && pin == confirmacion) {
                onConfirmar(pin)
            } else {
                noCoincide = true
            }
        },
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Text("Guardar")
    }
}

@Composable
private fun DesbloquearZonaPrivada(
    biometriaDisponible: Boolean,
    pinIncorrecto: Boolean,
    onIntentarPin: (String) -> Unit,
    onDesbloquearConBiometria: () -> Unit,
) {
    val context = LocalContext.current
    var mostrarCampoPin by remember { mutableStateOf(!biometriaDisponible) }
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity
        if (biometriaDisponible && activity != null) {
            mostrarPromptBiometrico(
                activity = activity,
                onExito = onDesbloquearConBiometria,
                onError = { mostrarCampoPin = true },
            )
        }
    }

    Text(text = "🔒 Confirma que eres tú", style = MaterialTheme.typography.titleLarge)

    if (!mostrarCampoPin) {
        Text(text = "Usa tu huella o rostro para continuar", modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = { mostrarCampoPin = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Usar PIN en su lugar")
        }
        return
    }

    OutlinedTextField(
        value = pin,
        onValueChange = { nuevo -> if (nuevo.length <= 6 && nuevo.all { it.isDigit() }) pin = nuevo },
        label = { Text("PIN") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    )
    if (pinIncorrecto) {
        Text(text = "PIN incorrecto, intenta de nuevo", modifier = Modifier.padding(top = 4.dp))
    }
    Button(onClick = { onIntentarPin(pin) }, modifier = Modifier.padding(top = 16.dp)) {
        Text("Entrar")
    }
}
