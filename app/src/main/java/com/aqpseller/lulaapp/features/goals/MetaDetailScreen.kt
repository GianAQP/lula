package com.aqpseller.lulaapp.features.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.CompartirActividadDialog
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.InvitacionEnviadaDialog
import com.aqpseller.lulaapp.core.ui.LulaProgressBar
import com.aqpseller.lulaapp.core.ui.SelectorFechaRapida
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.PermisoCompartir

@Composable
fun MetaDetailScreen(
    onEditar: (String) -> Unit,
    onEliminada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MetaDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val solicitudEnviada by viewModel.solicitudEnviada.collectAsState()
    var incrementoTexto by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
    var permisoPendiente by remember { mutableStateOf(PermisoCompartir.PUEDE_VER) }
    var invitacionEnviada by remember { mutableStateOf(false) }
    var mostrarAplazar by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.eliminada) { if (uiState.eliminada) onEliminada() }
    LaunchedEffect(Unit) { viewModel.recargar() }
    LaunchedEffect(solicitudEnviada) {
        if (solicitudEnviada) {
            invitacionEnviada = true
            viewModel.solicitudEnviadaMostrada()
        }
    }

    if (uiState.cargando) return

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = uiState.nombre, style = MaterialTheme.typography.titleLarge)
        LulaProgressBar(progreso = uiState.fraccionProgreso, modifier = Modifier.padding(top = 12.dp))
        Text(
            text = "${uiState.progreso.toInt()} de ${uiState.objetivo.toInt()}",
            modifier = Modifier.padding(top = 8.dp),
        )
        uiState.nombreHabitoVinculado?.let {
            Text(text = "Hábito vinculado: $it", modifier = Modifier.padding(top = 4.dp))
        }
        uiState.fechaLimite?.let { fechaLimite ->
            Text(
                text = "📅 ${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaLimite))}",
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = { mostrarAplazar = !mostrarAplazar }, modifier = Modifier.padding(top = 4.dp)) {
                Text("🔜 Aplazar")
            }
            if (mostrarAplazar) {
                SelectorFechaRapida(
                    fechaActual = uiState.fechaLimite,
                    onFechaElegida = { viewModel.aplazar(it); mostrarAplazar = false },
                )
            }
        }

        if (uiState.esManual) {
            OutlinedTextField(
                value = incrementoTexto,
                onValueChange = { nuevo -> if (nuevo.all { it.isDigit() || it == '.' }) incrementoTexto = nuevo },
                label = { Text("Agregar progreso") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
            Button(
                onClick = {
                    viewModel.agregarProgreso(incrementoTexto.toDoubleOrNull() ?: 0.0)
                    incrementoTexto = ""
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("+ Agregar progreso")
            }
        } else if (uiState.esPorMonto) {
            Text(
                text = "El progreso se calcula solo, sumando tus movimientos de categoría \"Ahorro\" en Finanzas.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            Text(
                text = "El progreso se calcula solo, según cuántos de los últimos ${uiState.objetivo.toInt()} días cumpliste el hábito.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            OutlinedButton(onClick = { onEditar(viewModel.metaId) }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Editar")
            }
            OutlinedButton(onClick = { mostrarConfirmacion = true }) {
                Text("Eliminar meta")
            }
        }
        OutlinedButton(onClick = { mostrarCompartir = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("🤝 Compartir seguimiento")
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina la meta \"${uiState.nombre}\" y su progreso para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminar() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }

    if (mostrarCompartir) {
        CompartirActividadDialog(
            nombreActividad = uiState.nombre,
            onEnviar = { contacto, permiso ->
                mostrarCompartir = false
                permisoPendiente = permiso
                viewModel.compartir(contacto, permiso)
            },
            onCancelar = { mostrarCompartir = false },
        )
    }

    if (invitacionEnviada) {
        InvitacionEnviadaDialog(
            nombreActividad = uiState.nombre,
            permiso = permisoPendiente,
            onCerrar = { invitacionEnviada = false },
        )
    }
}
