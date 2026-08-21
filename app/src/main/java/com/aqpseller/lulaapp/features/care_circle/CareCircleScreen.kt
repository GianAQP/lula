package com.aqpseller.lulaapp.features.care_circle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.domain.model.EstadoSolicitud
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.domain.model.TipoSolicitud

private fun etiquetaElemento(elemento: String, tipo: TipoSolicitud, permiso: PermisoCompartir): String = when (tipo) {
    TipoSolicitud.ESPACIO -> "🏠 Invitación a la Familia \"$elemento\""
    TipoSolicitud.ACTIVIDAD -> "$elemento — ${etiquetaPermiso(permiso)}"
}

private fun etiquetaPermiso(permiso: PermisoCompartir): String = when (permiso) {
    PermisoCompartir.PUEDE_VER -> "puede ver"
    PermisoCompartir.PUEDE_VER_Y_RECORDAR -> "puede ver y recordar"
}

private fun etiquetaEstado(estado: EstadoSolicitud): String = when (estado) {
    EstadoSolicitud.PENDIENTE -> "⏳ Pendiente de aceptar"
    EstadoSolicitud.ACEPTADA -> "✅ Aceptada"
    EstadoSolicitud.RECHAZADA -> "✋ Rechazada"
    EstadoSolicitud.ESPERANDO_INSTALACION -> "📲 Esperando que instale Lula"
}

@Composable
fun CareCircleScreen(
    modifier: Modifier = Modifier,
    viewModel: CareCircleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var solicitudACancelar by remember { mutableStateOf<SolicitudEnviadaUi?>(null) }

    if (uiState.cargando) return

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Mi círculo de cuidado",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                Text(text = "QUIÉN ME ACOMPAÑA A MÍ", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Personas a quienes compartiste el seguimiento de algo tuyo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
            }
            if (uiState.enviadas.isEmpty()) {
                item {
                    Text(
                        text = "Todavía no compartiste nada. Desde el detalle de un hábito, tarea o medicamento vas a encontrar \"🤝 Compartir seguimiento\".",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                items(uiState.enviadas, key = { "enviada-${it.id}" }) { solicitud ->
                    Card(
                        colors = CardDefaults.cardColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "👤 ${solicitud.contacto}", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = etiquetaElemento(solicitud.elemento, solicitud.tipo, solicitud.permiso),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                text = etiquetaEstado(solicitud.estado),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(onClick = { solicitudACancelar = solicitud }) {
                                    Text(if (solicitud.estado == EstadoSolicitud.PENDIENTE) "Cancelar solicitud" else "Revocar acceso")
                                }
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(text = "PERSONAS QUE ACOMPAÑO", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Solicitudes que alguien más te envió a ti.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
            }
            if (uiState.recibidas.isEmpty()) {
                item {
                    Text(
                        text = "Nada por ahora. Necesitas vincular tu cuenta con Google (Perfil → \"🔑 Cuenta\") para poder recibir solicitudes.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                items(uiState.recibidas, key = { "recibida-${it.id}" }) { solicitud ->
                    Card(
                        colors = CardDefaults.cardColors(),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "👤 ${solicitud.deNombre}", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = etiquetaElemento(solicitud.elemento, solicitud.tipo, solicitud.permiso),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(onClick = { viewModel.rechazar(solicitud.id) }) {
                                    Text("Rechazar")
                                }
                                Button(
                                    onClick = { viewModel.aceptar(solicitud.id) },
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Text("Aceptar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    solicitudACancelar?.let { solicitud ->
        ConfirmarEliminarDialog(
            mensaje = "Esto cancela el acceso de \"${solicitud.contacto}\" a \"${solicitud.elemento}\".",
            onConfirmar = { viewModel.cancelar(solicitud.id); solicitudACancelar = null },
            onCancelar = { solicitudACancelar = null },
        )
    }
}
