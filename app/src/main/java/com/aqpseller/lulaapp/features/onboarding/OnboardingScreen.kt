package com.aqpseller.lulaapp.features.onboarding

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.R
import com.aqpseller.lulaapp.core.auth.obtenerGoogleIdToken
import com.aqpseller.lulaapp.core.ui.LulaProgressBar
import com.aqpseller.lulaapp.domain.legal.TipoDocumentoLegal
import com.aqpseller.lulaapp.domain.legal.TextosLegales
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onTerminado: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val terminado by viewModel.terminado.collectAsState()

    LaunchedEffect(terminado) {
        if (terminado) onTerminado()
    }

    // A diferencia del resto de la app (siempre dentro del `Scaffold` de `LulaNavHost`, que ya
    // pinta su propio fondo), esta pantalla se muestra ANTES de `LulaNavHost` — sin `Surface`
    // acá, no hay fondo pintado ni color de texto correcto, y el texto queda gris casi invisible
    // sobre el fondo por defecto de la ventana. Ver `Plan/08-decisiones-tecnicas.md`.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val indice = PasoOnboarding.entries.indexOf(uiState.paso)
            if (uiState.paso != PasoOnboarding.BIENVENIDA) {
                LulaProgressBar(progreso = (indice + 1) / PasoOnboarding.entries.size.toFloat())
                Spacer(modifier = Modifier.height(24.dp))
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                when (uiState.paso) {
                    PasoOnboarding.BIENVENIDA -> PasoBienvenida(onEmpezar = viewModel::avanzar)
                    PasoOnboarding.CUENTA -> PasoCuenta(uiState = uiState, viewModel = viewModel)
                    PasoOnboarding.PRIVACIDAD -> PasoPrivacidad(uiState = uiState, viewModel = viewModel)
                    PasoOnboarding.QUE_MEJORAR -> PasoQueMejorar(uiState = uiState, viewModel = viewModel)
                    PasoOnboarding.COMO_EMPEZAR -> PasoComoEmpezar(uiState = uiState, viewModel = viewModel)
                    PasoOnboarding.MOMENTO_DEL_DIA -> PasoMomentoDelDia(uiState = uiState, viewModel = viewModel)
                    PasoOnboarding.COMO_LLAMARTE -> PasoComoLlamarte(uiState = uiState, viewModel = viewModel)
                    PasoOnboarding.POR_QUE_HOY -> PasoPorQueHoy(uiState = uiState, viewModel = viewModel)
                    PasoOnboarding.RESUMEN -> PasoResumen(uiState = uiState, onEmpezar = viewModel::finalizar)
                }
            }
        }
    }
}

@Composable
private fun PasoBienvenida(onEmpezar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌱", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Hola, soy Lula.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Vamos a organizar tu día a día, poco a poco.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEmpezar, modifier = Modifier.fillMaxWidth()) { Text("Empezar") }
    }
}

@Composable
private fun PasoCuenta(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("¿Cómo quieres entrar?", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))
        if (uiState.vinculandoCuenta) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val idToken = obtenerGoogleIdToken(context, context.getString(R.string.default_web_client_id))
                            viewModel.reclamarConGoogle(idToken)
                        } catch (e: GetCredentialException) {
                            Toast.makeText(context, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🔵 Continuar con Google") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("✉️ Continuar con correo (próximamente)")
        }
        uiState.errorCuenta?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Aceptas nuestros Términos y Política de Privacidad",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PasoPrivacidad(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    var mostrarTexto by remember { mutableStateOf(false) }
    Column {
        Text("Tus datos están seguros", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Guardamos tu información de forma segura. Tú decides qué compartir y con quién.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = uiState.privacidadAceptada, onCheckedChange = viewModel::aceptarPrivacidad)
            Text("He leído y acepto la Política de Privacidad", style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = { mostrarTexto = !mostrarTexto }) {
            Text(if (mostrarTexto) "Ocultar política" else "Ver política")
        }
        if (mostrarTexto) {
            Text(
                TextosLegales.textoPara(TipoDocumentoLegal.PRIVACIDAD),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::continuarDesdePrivacidad,
            enabled = uiState.privacidadAceptada,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continuar") }
    }
}

@Composable
private fun PasoQueMejorar(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    PasoPreguntaMultiple(
        pregunta = "¿Qué quieres mejorar primero?",
        ayuda = "Elige hasta 2",
        opciones = listOf("Organización", "Salud y hábitos", "Finanzas", "Lectura y aprendizaje"),
        seleccionadas = uiState.queMejorar,
        onSeleccionar = viewModel::alternarQueMejorar,
        onSiguiente = viewModel::avanzar,
    )
}

@Composable
private fun PasoComoEmpezar(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    PasoPreguntaUnica(
        pregunta = "¿Cómo prefieres empezar?",
        opciones = listOf("Con pocas cosas simples", "Con un plan más completo"),
        seleccionada = uiState.comoEmpezar,
        onSeleccionar = { viewModel.elegirComoEmpezar(it); viewModel.avanzar() },
    )
}

@Composable
private fun PasoMomentoDelDia(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    PasoPreguntaUnica(
        pregunta = "¿Qué momento del día usas más para organizarte?",
        opciones = listOf("Mañana", "Durante el día", "Noche"),
        seleccionada = uiState.momentoDelDia,
        onSeleccionar = { viewModel.elegirMomentoDelDia(it); viewModel.avanzar() },
    )
}

@Composable
private fun PasoComoLlamarte(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column {
        Text("¿Cómo prefieres que te hable Lula?", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.nombrePreferido,
            onValueChange = viewModel::cambiarNombrePreferido,
            label = { Text("Tu nombre") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Puedes escribir cómo prefieras que te llame",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = viewModel::avanzar, modifier = Modifier.fillMaxWidth()) { Text("Siguiente") }
    }
}

@Composable
private fun PasoPorQueHoy(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    PasoPreguntaUnica(
        pregunta = "¿Por qué quieres empezar hoy?",
        opciones = listOf("Quiero ser más constante", "Quiero organizar mi día a día", "Quiero controlar mis finanzas"),
        seleccionada = uiState.porQueHoy,
        onSeleccionar = { viewModel.elegirPorQueHoy(it); viewModel.avanzar() },
        permitirSaltar = true,
        onSiguiente = viewModel::avanzar,
    )
}

@Composable
private fun PasoResumen(uiState: OnboardingUiState, onEmpezar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✅", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Listo, ${uiState.nombrePreferido.ifBlank { "" }}.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Vamos paso a paso.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEmpezar, modifier = Modifier.fillMaxWidth()) { Text("Empezar mi día") }
    }
}

@Composable
private fun PasoPreguntaUnica(
    pregunta: String,
    opciones: List<String>,
    seleccionada: String?,
    onSeleccionar: (String) -> Unit,
    permitirSaltar: Boolean = false,
    onSiguiente: (() -> Unit)? = null,
) {
    Column {
        Text(pregunta, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        opciones.forEach { opcion ->
            OutlinedButton(
                onClick = { onSeleccionar(opcion) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text(opcion) }
        }
        if (permitirSaltar && onSiguiente != null) {
            TextButton(onClick = onSiguiente, modifier = Modifier.fillMaxWidth()) { Text("Omitir") }
        }
    }
}

@Composable
private fun PasoPreguntaMultiple(
    pregunta: String,
    ayuda: String,
    opciones: List<String>,
    seleccionadas: Set<String>,
    onSeleccionar: (String) -> Unit,
    onSiguiente: () -> Unit,
) {
    Column {
        Text(pregunta, style = MaterialTheme.typography.headlineSmall)
        Text(ayuda, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { opcion ->
                FilterChip(
                    selected = opcion in seleccionadas,
                    onClick = { onSeleccionar(opcion) },
                    label = { Text(opcion) },
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSiguiente, modifier = Modifier.fillMaxWidth()) { Text("Siguiente") }
    }
}
