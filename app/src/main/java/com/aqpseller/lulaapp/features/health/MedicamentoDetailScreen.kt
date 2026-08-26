package com.aqpseller.lulaapp.features.health

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.aqpseller.lulaapp.core.ui.CompartirActividadDialog
import com.aqpseller.lulaapp.core.ui.CompartirPorQrDialog
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.InvitacionEnviadaDialog
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir

private fun emojiEstado(estado: EstadoActividad): String = when (estado) {
    EstadoActividad.CONFIRMADO -> "✅"
    EstadoActividad.OMITIDO -> "⏭️"
    EstadoActividad.SIN_CONFIRMAR -> "⏳"
}

@Composable
fun MedicamentoDetailScreen(
    onEditar: (String) -> Unit,
    onEliminado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicamentoDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val solicitudEnviada by viewModel.solicitudEnviada.collectAsState()
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
    var mostrarCodigoQr by remember { mutableStateOf(false) }
    var permisoPendiente by remember { mutableStateOf(PermisoCompartir.PUEDE_VER) }
    var invitacionEnviada by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.eliminado) { if (uiState.eliminado) onEliminado() }
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
        Text(text = uiState.dosis, modifier = Modifier.padding(top = 8.dp))
        if (!uiState.activa) {
            Text(text = "Pausado", modifier = Modifier.padding(top = 8.dp))
        }

        Text(text = "Historial (últimos 7 días)", modifier = Modifier.padding(top = 16.dp))
        if (uiState.historial.isEmpty()) {
            Text(
                text = "Todavía no hay tomas registradas.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            uiState.historial.forEach { toma ->
                Text(
                    text = "${emojiEstado(toma.estado)} ${toma.fechaTexto} · ${toma.horario}",
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (uiState.nombresTareasVinculadas.isNotEmpty()) {
            Text(text = "📝 Tareas vinculadas", modifier = Modifier.padding(top = 16.dp))
            uiState.nombresTareasVinculadas.forEach { nombre ->
                Text(text = "· $nombre", modifier = Modifier.padding(top = 2.dp))
            }
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            Button(onClick = { onEditar(viewModel.actividadId) }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Editar")
            }
            OutlinedButton(onClick = viewModel::pausarOReanudar, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (uiState.activa) "Pausar" else "Reanudar")
            }
            OutlinedButton(onClick = { mostrarConfirmacion = true }) {
                Text("Eliminar")
            }
        }
        OutlinedButton(onClick = { mostrarCompartir = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("🤝 Compartir seguimiento")
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina \"${uiState.nombre}\" y su historial para siempre.",
            onConfirmar = { mostrarConfirmacion = false; viewModel.eliminar() },
            onCancelar = { mostrarConfirmacion = false },
        )
    }

    if (mostrarCompartir) {
        CompartirActividadDialog(
            nombreActividad = uiState.nombre,
            onElegirQr = { permiso ->
                mostrarCompartir = false
                permisoPendiente = permiso
                mostrarCodigoQr = true
                viewModel.generarCodigoQr(permiso)
            },
            onEnviar = { contacto, permiso ->
                mostrarCompartir = false
                permisoPendiente = permiso
                viewModel.compartir(contacto, permiso)
            },
            onCancelar = { mostrarCompartir = false },
        )
    }

    if (mostrarCodigoQr) {
        val estadoQr by viewModel.estadoCompartirQr.collectAsState()
        CompartirPorQrDialog(
            nombreActividad = uiState.nombre,
            estado = estadoQr,
            onReintentar = { viewModel.generarCodigoQr(permisoPendiente) },
            onCerrar = { mostrarCodigoQr = false; viewModel.ocultarCodigoQr() },
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
