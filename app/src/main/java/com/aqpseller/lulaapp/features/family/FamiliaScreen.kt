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
import androidx.compose.ui.Alignment
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
    var mostrarConfirmarSalir by remember { mutableStateOf(false) }
    var miembroAQuitar by remember { mutableStateOf<MiembroUi?>(null) }
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

        Text(text = "Tus espacios familiares", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "Puedes tener más de uno — por ejemplo la familia que formaste (pareja e hijos), " +
                "la de tus padres y hermanos, y la de tu pareja. Cada una es independiente: sus " +
                "propios miembros, tareas del hogar y retos.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )

        uiState.familias.forEach { familia ->
            val estaSeleccionada = uiState.familiaSeleccionadaId == familia.id
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "👨‍👩‍👧 ${familia.nombre}", modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        if (estaSeleccionada) viewModel.cerrarFamiliaSeleccionada() else viewModel.seleccionarFamilia(familia.id, familia.nombre)
                    },
                ) { Text(if (estaSeleccionada) "Ocultar" else "Administrar") }
            }
        }

        if (!uiState.mostrarFormularioCrear) {
            Button(onClick = viewModel::mostrarFormularioCrear, modifier = Modifier.padding(top = 12.dp)) {
                Text(if (uiState.familias.isEmpty()) "+ Crear espacio familiar" else "+ Crear otro espacio familiar")
            }
        } else {
            Text(text = "Crear espacio familiar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            DictationTextField(
                value = nombreNuevoEspacio,
                onValueChange = { nombreNuevoEspacio = it },
                label = "Nombre (ej. \"Familia García\" o \"Familia de mis papás\")",
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = { viewModel.crearEspacioFamilia(nombreNuevoEspacio); nombreNuevoEspacio = "" },
                    modifier = Modifier.padding(end = 8.dp),
                ) { Text("Crear") }
                TextButton(onClick = viewModel::ocultarFormularioCrear) { Text("Cancelar") }
            }
        }

        if (uiState.familiaSeleccionadaId != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            if (uiState.mostrarFormularioRenombrar) {
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
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🙋 ${miembro.nombre} — ${miembro.rol}", modifier = Modifier.weight(1f))
                        if (uiState.soyAdmin && !miembro.esUnoMismo) {
                            TextButton(onClick = { miembroAQuitar = miembro }) { Text("Quitar") }
                        }
                    }
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
                        label = "Correo de la persona",
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
                // Retos familiares siempre es del espacio ACTIVO (arriba), no de la Familia que
                // estás administrando acá si son distintas — evita mostrar retos de la familia
                // equivocada. Ver `Plan/10-pendientes.md`.
                val esLaActiva = uiState.espacios.find { it.esActivo }?.id == uiState.familiaSeleccionadaId
                if (esLaActiva) {
                    TextButton(
                        onClick = { onVerRetosFamiliares() },
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    ) {
                        Text("🏆 Retos familiares")
                    }
                } else {
                    Text(
                        text = "Cambia a este espacio (arriba, \"Tus espacios\") para ver sus retos familiares.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = viewModel::mostrarFormularioRenombrar, modifier = Modifier.padding(end = 8.dp)) {
                        Text("✏️ Renombrar")
                    }
                    OutlinedButton(onClick = { mostrarConfirmarEliminar = true }) {
                        Text("🗑️ Eliminar")
                    }
                }
                TextButton(onClick = { mostrarConfirmarSalir = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("🚪 Salir de este espacio")
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
    if (mostrarConfirmarSalir) {
        ConfirmarEliminarDialog(
            mensaje = "Vas a salir de \"${uiState.nombreEspacioFamilia}\". Lo que ya se creó ahí se " +
                "queda tal cual para el resto — solo dejas de tener acceso tú.",
            onConfirmar = { mostrarConfirmarSalir = false; viewModel.salirDeEspacioFamilia() },
            onCancelar = { mostrarConfirmarSalir = false },
        )
    }
    miembroAQuitar?.let { miembro ->
        ConfirmarEliminarDialog(
            mensaje = "Vas a quitar a \"${miembro.nombre}\" de \"${uiState.nombreEspacioFamilia}\". Lo que ya " +
                "creó ahí se queda tal cual — solo pierde el acceso.",
            onConfirmar = { viewModel.eliminarMiembro(miembro); miembroAQuitar = null },
            onCancelar = { miembroAQuitar = null },
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
