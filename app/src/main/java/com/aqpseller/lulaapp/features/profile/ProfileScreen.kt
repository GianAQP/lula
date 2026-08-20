package com.aqpseller.lulaapp.features.profile

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.R
import com.aqpseller.lulaapp.core.auth.obtenerGoogleIdToken
import com.aqpseller.lulaapp.core.ui.ConfirmarEliminarDialog
import com.aqpseller.lulaapp.core.ui.HoraSelector
import com.aqpseller.lulaapp.core.ui.SectionLinkRow
import com.aqpseller.lulaapp.core.ui.TarjetaSeccion
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.reiniciarApp
import com.aqpseller.lulaapp.domain.legal.TipoDocumentoLegal
import com.aqpseller.lulaapp.domain.model.MetodoLogin
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaFamiliaContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerLight
import kotlinx.coroutines.launch

/**
 * "Mi perfil" (CUENTA en el menú "⋮"), separada de Ajustes (CONFIGURACIÓN) por `02-pantallas.md`.
 * Hoy solo edita horarios de comida — antes solo se podían fijar la primera vez, de paso, dentro
 * de "Crear medicamento". Nombre/correo son de solo lectura porque todavía no hay onboarding real
 * (usuario semilla local, ver `Plan/08-decisiones-tecnicas.md`). Reagrupada en tarjetas (mismo
 * patrón `TarjetaSeccion` que Ajustes) y con acceso directo a Círculo de cuidado y Familia/Espacios
 * — antes solo se llegaba ahí desde el menú "⋮" o el bottom bar, no desde Perfil.
 */
@Composable
fun ProfileScreen(
    onVerProposito: () -> Unit,
    onVerTextoLegal: (tipo: String) -> Unit,
    onVerCirculoCuidado: () -> Unit,
    onVerFamilia: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val usuario by viewModel.usuario.collectAsState()
    val mensajeCuenta by viewModel.mensajeCuenta.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mostrarConfirmarEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(mensajeCuenta) {
        mensajeCuenta?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.mensajeCuentaMostrado()
        }
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Mi perfil", style = MaterialTheme.typography.titleLarge)

        usuario?.let { u ->
            Text(
                text = u.nombrePreferido,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            u.correo?.let { correo ->
                Text(text = correo, style = MaterialTheme.typography.bodySmall)
            }
        }

        TarjetaSeccion(titulo = "🔑 Cuenta") {
            if (usuario?.metodoLogin == MetodoLogin.GOOGLE) {
                Text(
                    text = "✅ Vinculada con Google",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "Necesaria para compartir con Familia y Círculo de cuidado entre dispositivos.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                OutlinedButton(
                    onClick = viewModel::cerrarSesionGoogle,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                ) {
                    Text("Cerrar sesión de Google")
                }
            } else {
                Text(
                    text = "Tu cuenta vive solo en este dispositivo. Vincúlala con Google para " +
                        "poder compartir con Familia y Círculo de cuidado.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val idToken = obtenerGoogleIdToken(context, context.getString(R.string.default_web_client_id))
                                viewModel.reclamarCuentaConGoogle(idToken)
                            } catch (e: GetCredentialException) {
                                Toast.makeText(context, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                ) {
                    Text("🔵 Continuar con Google")
                }
            }
        }

        TarjetaSeccion(titulo = "🧭 Mi crecimiento") {
            SectionLinkRow(
                emoji = "🧭",
                color = LulaPrimaryContainerLight,
                texto = "Mi propósito",
                onClick = onVerProposito,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        TarjetaSeccion(titulo = "👥 Mi espacio") {
            SectionLinkRow(
                emoji = "👥",
                color = LulaAsistenteContainerLight,
                texto = "Círculo de cuidado",
                onClick = onVerCirculoCuidado,
                modifier = Modifier.padding(top = 8.dp),
            )
            SectionLinkRow(
                emoji = "👨‍👩‍👧",
                color = LulaFamiliaContainerLight,
                texto = "Familia / Espacios",
                onClick = onVerFamilia,
            )
        }

        TarjetaSeccion(titulo = "🍽️ Horarios de comida") {
            Text(
                text = "Lula los usa para calcular la hora de tus medicamentos \"según las comidas\". " +
                    "Si cambias un horario acá, los medicamentos que ya creaste no se mueven solos — " +
                    "hay que volver a abrirlos y guardarlos para que tomen la hora nueva.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )

            Text(text = "🌅 Desayuno", modifier = Modifier.padding(top = 16.dp))
            HoraSelector(
                hora = usuario?.horaDesayuno,
                onHoraSeleccionada = viewModel::actualizarHorarioDesayuno,
                modifier = Modifier.padding(top = 4.dp),
            )

            Text(text = "🍲 Almuerzo", modifier = Modifier.padding(top = 16.dp))
            HoraSelector(
                hora = usuario?.horaAlmuerzo,
                onHoraSeleccionada = viewModel::actualizarHorarioAlmuerzo,
                modifier = Modifier.padding(top = 4.dp),
            )

            Text(text = "🌙 Cena", modifier = Modifier.padding(top = 16.dp))
            HoraSelector(
                hora = usuario?.horaCena,
                onHoraSeleccionada = viewModel::actualizarHorarioCena,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        usuario?.let { u ->
            TarjetaSeccion(titulo = "🔒 Privacidad y legal") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = u.confirmoMayorDe13,
                        onCheckedChange = { if (it) viewModel.confirmarMayorDe13() },
                        enabled = !u.confirmoMayorDe13,
                    )
                    Text("Confirmo que soy mayor de 13 años")
                }

                ConsentimientoRow(
                    etiqueta = "Términos de servicio",
                    aceptadoEn = u.terminosAceptadosEn,
                    onClick = { onVerTextoLegal(TipoDocumentoLegal.TERMINOS.id) },
                )

                ConsentimientoRow(
                    etiqueta = "Datos de salud (medicamentos, citas)",
                    aceptadoEn = u.consentimientoDatosSaludEn,
                    onClick = { onVerTextoLegal(TipoDocumentoLegal.DATOS_SALUD.id) },
                )

                ConsentimientoRow(
                    etiqueta = "Política de privacidad",
                    aceptadoEn = u.privacidadAceptadaEn,
                    onClick = { onVerTextoLegal(TipoDocumentoLegal.PRIVACIDAD.id) },
                )
            }
        }

        TarjetaSeccion(titulo = "⚠️ Zona de peligro") {
            Text(
                text = "Borra todos tus datos de este dispositivo: hábitos, tareas, finanzas, " +
                    "diario, propósito, todo. No se puede deshacer.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                onClick = { mostrarConfirmarEliminar = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            ) {
                Text("🗑️ Eliminar mi cuenta")
            }
        }
    }

    if (mostrarConfirmarEliminar) {
        ConfirmarEliminarDialog(
            mensaje = "Se borrarán todos tus datos de este dispositivo de forma permanente. " +
                "Esta acción no se puede deshacer.",
            onConfirmar = {
                mostrarConfirmarEliminar = false
                viewModel.eliminarCuenta { reiniciarApp(context) }
            },
            onCancelar = { mostrarConfirmarEliminar = false },
        )
    }
}

@Composable
private fun ConsentimientoRow(
    etiqueta: String,
    aceptadoEn: Long?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(etiqueta)
            if (aceptadoEn != null) {
                Text(
                    text = "✅ Aceptado el " +
                        DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(aceptadoEn)),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(text = "Pendiente", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(text = if (aceptadoEn == null) "Leer y aceptar →" else "Ver →", style = MaterialTheme.typography.bodySmall)
    }
}
