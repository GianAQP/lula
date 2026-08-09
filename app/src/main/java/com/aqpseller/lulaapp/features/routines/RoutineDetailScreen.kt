package com.aqpseller.lulaapp.features.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.CompartirActividadDialog
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.InvitacionEnviadaDialog
import com.aqpseller.lulaapp.core.ui.SonidoCheckViewModel
import com.aqpseller.lulaapp.core.utils.SonidoUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir

@Composable
fun RoutineDetailScreen(
    onEditar: (String) -> Unit,
    onEliminada: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineDetailViewModel = hiltViewModel(),
    sonidoCheckViewModel: SonidoCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sonidoCheckHabilitado by sonidoCheckViewModel.habilitado.collectAsState()
    val solicitudEnviada by viewModel.solicitudEnviada.collectAsState()
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
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

        val completadas = uiState.items.count { it.estado == EstadoActividad.CONFIRMADO }
        Text(
            text = "$completadas de ${uiState.items.size} completadas hoy",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        uiState.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val marcado = item.estado == EstadoActividad.CONFIRMADO
                Checkbox(
                    checked = marcado,
                    onCheckedChange = { nuevoMarcado ->
                        viewModel.marcarItem(item.id, nuevoMarcado)
                        if (sonidoCheckHabilitado) SonidoUtils.reproducirCheck()
                    },
                )
                Text(text = item.nombre, textDecoration = if (marcado) TextDecoration.LineThrough else null)
            }
        }

        Button(
            onClick = viewModel::marcarTodaCompleta,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("✅ Marcar rutina completa")
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            OutlinedButton(onClick = { onEditar(viewModel.actividadId) }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Editar")
            }
            OutlinedButton(onClick = { mostrarConfirmacion = true }) {
                Text("Eliminar rutina")
            }
        }
        OutlinedButton(onClick = { mostrarCompartir = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("🤝 Compartir seguimiento")
        }
    }

    if (mostrarConfirmacion) {
        ConfirmarEliminarDialog(
            mensaje = "Esto elimina la rutina \"${uiState.nombre}\" — los hábitos/tareas que agrupa NO se eliminan.",
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
