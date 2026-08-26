package com.aqpseller.lulaapp.features.tasks

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
import com.aqpseller.lulaapp.core.ui.etiquetaRecurrenciaTarea
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.RecurrenciaTarea

@Composable
fun TaskDetailScreen(
    onEditar: (String) -> Unit,
    onEliminada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val solicitudEnviada by viewModel.solicitudEnviada.collectAsState()
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
    var mostrarCodigoQr by remember { mutableStateOf(false) }
    var permisoPendiente by remember { mutableStateOf(PermisoCompartir.PUEDE_VER) }
    var invitacionEnviada by remember { mutableStateOf(false) }
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
        Text(
            text = if (uiState.estado == EstadoActividad.CONFIRMADO) "Completada" else "Pendiente",
            modifier = Modifier.padding(top = 8.dp),
        )
        if (uiState.estado == EstadoActividad.CONFIRMADO && uiState.fechaCompletado != null) {
            Text(
                text = "Completada el ${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(uiState.fechaCompletado!!))}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        uiState.fechaLimite?.let { fechaLimite ->
            val vencida = uiState.estado != EstadoActividad.CONFIRMADO && fechaLimite < DateTimeUtils.inicioDeHoyEpochMillis()
            Text(
                text = "📅 Fecha límite: ${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaLimite))}" +
                    if (vencida) " (vencida)" else "",
                color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.importante || uiState.urgente) {
            Text(
                text = listOfNotNull(
                    "Importante".takeIf { uiState.importante },
                    "Urgente".takeIf { uiState.urgente },
                ).joinToString(" · "),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.recurrencia != RecurrenciaTarea.SIN_REPETIR) {
            Text(
                text = "🔁 Se repite: ${etiquetaRecurrenciaTarea(uiState.recurrencia)}",
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        uiState.nombreActividadVinculada?.let { nombreVinculada ->
            Text(
                text = "🔗 Vinculada a: $nombreVinculada",
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Se marca como completada sola cuando eso termine.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            Button(onClick = viewModel::alternarCompletada, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (uiState.estado == EstadoActividad.CONFIRMADO) "Marcar pendiente" else "Marcar completada")
            }
            OutlinedButton(onClick = { onEditar(viewModel.actividadId) }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Editar")
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
            mensaje = "Esto elimina \"${uiState.nombre}\" para siempre.",
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
