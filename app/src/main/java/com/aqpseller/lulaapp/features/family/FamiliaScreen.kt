package com.aqpseller.lulaapp.features.family

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.DictationTextField

@Composable
fun FamiliaScreen(
    onEspacioCambiado: () -> Unit,
    onVerRetosFamiliares: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FamiliaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var nombreNuevoEspacio by remember { mutableStateOf("") }
    var nombreRenombrar by remember { mutableStateOf("") }
    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.espacioCambiado) { if (uiState.espacioCambiado) onEspacioCambiado() }
    LaunchedEffect(uiState.mostrarFormularioRenombrar) {
        if (uiState.mostrarFormularioRenombrar) nombreRenombrar = uiState.nombreEspacioFamilia
    }

    if (uiState.cargando) return

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Familia / Espacios", style = MaterialTheme.typography.titleLarge)

        Text(text = "Tus espacios", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
        Text(
            text = "Toca uno para cambiarte — todo lo que veas en la app (Hoy, Tareas, Finanzas, " +
                "Calendario) va a ser de ese espacio hasta que cambies de nuevo.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        uiState.espacios.forEach { espacio ->
            Row(modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = espacio.esActivo,
                    onClick = { if (!espacio.esActivo) viewModel.seleccionarEspacio(espacio.id) },
                    label = { Text((if (espacio.esFamilia) "👨‍👩‍👧 " else "👤 ") + espacio.nombre + if (espacio.esActivo) " (actual)" else "") },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        if (uiState.espacioFamiliaId == null) {
            if (!uiState.mostrarFormularioCrear) {
                Text(
                    text = "Todavía no tienes un espacio familiar. Ahí van a vivir las tareas del " +
                        "hogar y los retos familiares — por ahora es solo para ti, hasta que se " +
                        "puedan invitar personas de verdad.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = viewModel::mostrarFormularioCrear, modifier = Modifier.padding(top = 12.dp)) {
                    Text("+ Crear espacio familiar")
                }
            } else {
                Text(text = "Crear espacio familiar", style = MaterialTheme.typography.titleSmall)
                DictationTextField(
                    value = nombreNuevoEspacio,
                    onValueChange = { nombreNuevoEspacio = it },
                    label = "Nombre (ej. \"Familia García\")",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    Button(
                        onClick = { viewModel.crearEspacioFamilia(nombreNuevoEspacio) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Crear") }
                    TextButton(onClick = viewModel::ocultarFormularioCrear) { Text("Cancelar") }
                }
            }
        } else if (uiState.mostrarFormularioRenombrar) {
            Text(text = "Renombrar espacio familiar", style = MaterialTheme.typography.titleSmall)
            DictationTextField(
                value = nombreRenombrar,
                onValueChange = { nombreRenombrar = it },
                label = "Nombre",
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = { viewModel.renombrarEspacioFamilia(nombreRenombrar) },
                    modifier = Modifier.padding(end = 8.dp),
                ) { Text("Guardar") }
                TextButton(onClick = viewModel::ocultarFormularioRenombrar) { Text("Cancelar") }
            }
        } else {
            Text(text = "👨‍👩‍👧 ${uiState.nombreEspacioFamilia}", style = MaterialTheme.typography.titleSmall)
            Text(text = "Miembros", modifier = Modifier.padding(top = 12.dp))
            uiState.miembros.forEach { miembro ->
                Text(text = "🙋 ${miembro.nombre} — ${miembro.rol}", modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                text = "Invitar a alguien de verdad todavía no se puede — necesita que exista tu " +
                    "cuenta y la de la otra persona conectadas, algo que Lula no tiene aún.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = onVerRetosFamiliares, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                Text("🏆 Retos familiares")
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = viewModel::mostrarFormularioRenombrar, modifier = Modifier.padding(end = 8.dp)) {
                    Text("✏️ Renombrar")
                }
                OutlinedButton(onClick = { mostrarConfirmarEliminar = true }) {
                    Text("🗑️ Eliminar")
                }
            }
        }
    }

    if (mostrarConfirmarEliminar) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina \"${uiState.nombreEspacioFamilia}\" y todo lo que creaste ahí " +
                "(tareas, hábitos, metas, listas, movimientos financieros, retos familiares) para siempre.",
            onConfirmar = { mostrarConfirmarEliminar = false; viewModel.eliminarEspacioFamilia() },
            onCancelar = { mostrarConfirmarEliminar = false },
        )
    }
}
