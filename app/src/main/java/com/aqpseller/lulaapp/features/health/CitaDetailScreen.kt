package com.aqpseller.lulaapp.features.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.CompartirActividadDialog
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.InvitacionEnviadaDialog
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.PermisoCompartir
import com.aqpseller.lulaapp.ui.theme.LulaHabito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitaDetailScreen(
    onEditar: (String) -> Unit,
    onEliminado: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CitaDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val solicitudEnviada by viewModel.solicitudEnviada.collectAsState()
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarCompartir by remember { mutableStateOf(false) }
    var permisoPendiente by remember { mutableStateOf(PermisoCompartir.PUEDE_VER) }
    var invitacionEnviada by remember { mutableStateOf(false) }
    var sesionAReprogramar by remember { mutableStateOf<Int?>(null) }
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
        if (!uiState.esCurso) {
            Text(
                text = "📅 ${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(uiState.fechaHora))} · " +
                    DateTimeUtils.horaHHmm(uiState.fechaHora),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        uiState.lugar?.let { Text(text = "📍 $it", modifier = Modifier.padding(top = 4.dp)) }
        uiState.motivo?.let { Text(text = it, modifier = Modifier.padding(top = 8.dp)) }

        if (uiState.nombresTareasVinculadas.isNotEmpty()) {
            Text(text = "📝 Tareas vinculadas", modifier = Modifier.padding(top = 16.dp))
            uiState.nombresTareasVinculadas.forEach { nombre ->
                Text(text = "· $nombre", modifier = Modifier.padding(top = 2.dp))
            }
        }

        if (uiState.esCurso) {
            val cumplidas = uiState.sesiones.count { it.estado == EstadoActividad.CONFIRMADO }
            val total = uiState.cantidadSesionesTotal
            Text(
                text = "🔁 " + (if (total != null) "Van $cumplidas de $total sesiones" else "$cumplidas sesiones cumplidas"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Column(modifier = Modifier.padding(top = 8.dp)) {
                HorizontalDivider()
                uiState.sesiones.forEach { sesion ->
                    FilaSesionCita(
                        sesion = sesion,
                        onMarcarCumplida = { viewModel.marcarSesion(sesion.numeroSesion, EstadoActividad.CONFIRMADO) },
                        onMarcarOmitida = { viewModel.marcarSesion(sesion.numeroSesion, EstadoActividad.OMITIDO) },
                        onDeshacer = { viewModel.marcarSesion(sesion.numeroSesion, EstadoActividad.SIN_CONFIRMAR) },
                        onReprogramar = { sesionAReprogramar = sesion.numeroSesion },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            when (uiState.estado) {
                EstadoActividad.CONFIRMADO -> Text(text = "✅ Ya fuiste / se cumplió", modifier = Modifier.padding(top = 16.dp))
                EstadoActividad.OMITIDO -> Text(text = "⏭️ No se cumplió / se postergó", modifier = Modifier.padding(top = 16.dp))
                EstadoActividad.SIN_CONFIRMAR -> {
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Button(onClick = viewModel::marcarCumplida, modifier = Modifier.padding(end = 8.dp)) {
                            Text("✅ Marcar como cumplida")
                        }
                        OutlinedButton(onClick = viewModel::marcarOmitida) {
                            Text("⏭️ No se cumplió")
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            Button(onClick = { onEditar(viewModel.actividadId) }, modifier = Modifier.padding(end = 8.dp)) {
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

    val numeroSesionAReprogramar = sesionAReprogramar
    if (numeroSesionAReprogramar != null) {
        val sesion = uiState.sesiones.first { it.numeroSesion == numeroSesionAReprogramar }
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = DateTimeUtils.inicioDeDiaLocalAUtcMillis(DateTimeUtils.epochDiasAEpochMillis(sesion.fecha)),
        )
        DatePickerDialog(
            onDismissRequest = { sesionAReprogramar = null },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { utcMillis ->
                        val nuevaFechaEpochDay = DateTimeUtils.epochMillisToLocalDate(DateTimeUtils.utcMillisAInicioDeDiaLocal(utcMillis)).toEpochDays().toLong()
                        viewModel.reprogramarSesion(numeroSesionAReprogramar, nuevaFechaEpochDay)
                    }
                    sesionAReprogramar = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { sesionAReprogramar = null }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

@Composable
private fun FilaSesionCita(
    sesion: SesionCitaUi,
    onMarcarCumplida: () -> Unit,
    onMarcarOmitida: () -> Unit,
    onDeshacer: () -> Unit,
    onReprogramar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horaActual = DateTimeUtils.horaHHmm(DateTimeUtils.ahoraEpochMillis())
    val hoyEpochDay = DateTimeUtils.hoy().toEpochDays().toLong()
    // Vencida = ya pasó su fecha/hora y sigue sin marcar — mismo criterio que el resto de
    // la app (Hoy/Calendario), para que una sesión que quedó pendiente llame la atención en
    // vez de perderse en una lista larga donde antes todo se veía igual.
    val vencida = sesion.estado == EstadoActividad.SIN_CONFIRMAR &&
        (sesion.fecha < hoyEpochDay || (sesion.fecha == hoyEpochDay && sesion.horario < horaActual))
    // Una sesión futura (todavía no llega su día) no tiene nada que marcar todavía — antes
    // mostraba igual el botón "✅ Cumplida" en TODAS las sesiones sin marcar, y con un curso
    // largo (ej. 20 sesiones de radioterapia) eso se veía como una fila de checks verdes,
    // como si ya estuvieran hechas (confusión real reportada por el usuario). Solo se ofrece
    // marcar hoy o atrás; una futura solo se puede reprogramar.
    val esFutura = sesion.fecha > hoyEpochDay
    // La de hoy, todavía sin marcar y sin vencer, se resalta con fondo — antes se veía IGUAL que
    // una futura (blanco) o que una ya vencida (roja), sin ninguna pista de "esta es la que toca
    // ahora". A pedido del usuario. Ver `Plan/08-decisiones-tecnicas.md`.
    val esHoySinMarcar = sesion.fecha == hoyEpochDay && sesion.estado == EstadoActividad.SIN_CONFIRMAR && !vencida
    // Antes esto era solo un emoji "⬜"/"✅" de texto al inicio de la fila — parecía un
    // checkbox tocable (invitaba a tocarlo) pero no hacía nada; marcar de verdad requería
    // encontrar un botón de texto aparte más abajo, confuso (reportado por el usuario). Para
    // una sesión que sí se puede marcar (no futura, no omitida) ahora es un `Checkbox` real:
    // tocarlo SÍ marca/desmarca. Futura (no se puede marcar todavía) y Omitida (no es un
    // simple sí/no) siguen con el emoji fijo, sin fingir que se puede tocar. Ver
    // `Plan/08-decisiones-tecnicas.md`.
    val puedeUsarCheckbox = !esFutura && sesion.estado != EstadoActividad.OMITIDO
    // El check marcado usa el mismo verde que el resto de la app (antes usaba el violeta
    // primario de Material, que se confundía con el color de acento genérico) — igual que el
    // texto de una sesión ya cumplida: en vez de pintarlo morado, se tacha y se apaga, mismo
    // lenguaje que "Ya hechos hoy" en Hoy. Ver `08-decisiones-tecnicas.md`.
    val colorTexto = when {
        vencida -> MaterialTheme.colorScheme.error
        sesion.estado == EstadoActividad.CONFIRMADO -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.Unspecified
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .let { if (esHoySinMarcar) it.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)) else it }
            .padding(horizontal = if (esHoySinMarcar) 8.dp else 0.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (puedeUsarCheckbox) {
                Checkbox(
                    checked = sesion.estado == EstadoActividad.CONFIRMADO,
                    onCheckedChange = { marcado -> if (marcado) onMarcarCumplida() else onDeshacer() },
                    colors = CheckboxDefaults.colors(checkedColor = LulaHabito, checkmarkColor = Color.White),
                )
            } else {
                Text(text = if (sesion.estado == EstadoActividad.OMITIDO) "⏭️" else "⬜", modifier = Modifier.padding(end = 12.dp))
            }
            Text(
                text = "Sesión ${sesion.numeroSesion} — " +
                    "${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochDaysToLocalDate(sesion.fecha))} · ${sesion.horario}" +
                    (if (vencida) " ⚠️" else if (esFutura) " · pendiente" else ""),
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (sesion.estado == EstadoActividad.CONFIRMADO) TextDecoration.LineThrough else null,
                color = colorTexto,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                when {
                    sesion.estado == EstadoActividad.SIN_CONFIRMAR && !esFutura -> {
                        TextButton(onClick = onMarcarOmitida, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("⏭️ No se cumplió") }
                    }
                    sesion.estado == EstadoActividad.OMITIDO -> {
                        TextButton(onClick = onDeshacer, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("↩️ Deshacer") }
                    }
                }
            }
            // A la derecha a propósito — antes competía a la izquierda con "No se cumplió"/
            // "Deshacer", como si fueran del mismo grupo de acciones, cuando reprogramar es algo
            // aparte (a pedido del usuario). Ver `08-decisiones-tecnicas.md`.
            TextButton(onClick = onReprogramar, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("📅 Reprogramar") }
        }
    }
}
