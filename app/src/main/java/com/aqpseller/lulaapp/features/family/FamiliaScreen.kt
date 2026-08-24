package com.aqpseller.lulaapp.features.family

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.aqpseller.lulaapp.core.ui.InvitacionQrDialog
import com.aqpseller.lulaapp.core.utils.QrCodeGenerator

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
    var contactoInvitar by remember { mutableStateOf("") }
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
            if (!uiState.cuentaVinculada) {
                Text(
                    text = "Para invitar a alguien de verdad, primero vincula tu cuenta con Google " +
                        "(Perfil → \"🔑 Cuenta\").",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else if (uiState.mostrarFormularioInvitar) {
                DictationTextField(
                    value = contactoInvitar,
                    onValueChange = { contactoInvitar = it },
                    label = "Correo de la persona (o pega el que escaneaste con \"🔳\" arriba)",
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Button(
                        onClick = { viewModel.invitar(contactoInvitar); contactoInvitar = "" },
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Enviar invitación") }
                    TextButton(onClick = viewModel::ocultarFormularioInvitar) { Text("Cancelar") }
                }
            } else {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Button(onClick = viewModel::mostrarFormularioInvitar, modifier = Modifier.padding(end = 8.dp)) {
                        Text("+ Invitar a alguien")
                    }
                    OutlinedButton(onClick = viewModel::mostrarCodigoQr) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Que escaneen para entrar")
                    }
                }
            }
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
    if (uiState.mostrarInvitacionEnviada) {
        InvitacionQrDialog(
            titulo = "Invitación enviada",
            mensaje = "Todavía es \"Pendiente\" hasta que la otra persona instale Lula y acepte. " +
                "Mientras tanto, avísale así:",
            textoInvitacion = "🏡 Te invito a unirte a la Familia \"${uiState.nombreEspacioFamilia}\" en Lula " +
                "— vamos a compartir tareas del hogar y retos. La app todavía no está en la tienda; " +
                "escríbeme y coordinamos cómo instalarla.",
            onCerrar = viewModel::ocultarInvitacionEnviada,
        )
    }
    if (uiState.mostrarCodigoQr) {
        val textoQr = uiState.codigoQrTexto
        val qr = remember(textoQr) { textoQr?.let { QrCodeGenerator.generar(it) } }
        AlertDialog(
            onDismissRequest = viewModel::ocultarCodigoQr,
            confirmButton = { TextButton(onClick = viewModel::ocultarCodigoQr) { Text("Listo") } },
            title = { Text("Que escaneen para entrar") },
            text = {
                Column {
                    Text(
                        text = "Con el botón de escanear de su Lula — quedan dentro de \"${uiState.nombreEspacioFamilia}\" " +
                            "al instante, sin ningún paso más. Este código se renueva solo cada minuto, así que " +
                            "guardarlo no sirve para usarlo después.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    if (qr != null) {
                        Image(bitmap = qr, contentDescription = null, modifier = Modifier.size(220.dp))
                    } else {
                        Text("Generando código nuevo…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
        )
    }
}
