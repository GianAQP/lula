package com.aqpseller.lulaapp.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aqpseller.lulaapp.core.ui.HoraSelector
import com.aqpseller.lulaapp.core.ui.SectionLinkRow
import com.aqpseller.lulaapp.core.ui.TarjetaSeccion
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.abrirAjustesDeAlarmasExactas
import com.aqpseller.lulaapp.core.utils.abrirAjustesDeNotificaciones
import com.aqpseller.lulaapp.core.utils.abrirAjustesDeOptimizacionBateria
import com.aqpseller.lulaapp.core.utils.alarmasExactasPermitidas
import com.aqpseller.lulaapp.core.utils.exentoDeOptimizacionBateria
import com.aqpseller.lulaapp.core.utils.notificacionesPermitidas
import com.aqpseller.lulaapp.domain.model.MomentoDelDia
import com.aqpseller.lulaapp.navigation.OpcionBottomBar
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerLight

private val LETRAS_DIA_ISO = listOf("L", "M", "M", "J", "V", "S", "D")
private val DURACIONES_MAXIMAS_ALARMA_MIN = listOf(1, 5, 10, 15, 20, 25, null)

/**
 * Ajustes personales — accesible desde el menú "⋮" en Hoy, no mezclado con el contenido diario.
 * Agrupado en tarjetas por tema (antes era un solo scroll largo sin ninguna separación visual
 * entre secciones) — pedido explícito del usuario mostrando capturas de otras apps como
 * referencia. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sonidoCheckHabilitado by viewModel.sonidoCheckHabilitado.collectAsState()
    val diaRevisionSemanal by viewModel.diaRevisionSemanal.collectAsState()
    val horaRecordatorioCierreDia by viewModel.horaRecordatorioCierreDia.collectAsState()
    val horaRecordatorioManana by viewModel.horaRecordatorioManana.collectAsState()
    val horaRecordatorioTarde by viewModel.horaRecordatorioTarde.collectAsState()
    val horaRecordatorioNoche by viewModel.horaRecordatorioNoche.collectAsState()
    val bottomBarPosicion2 by viewModel.bottomBarPosicion2.collectAsState()
    val bottomBarPosicion3 by viewModel.bottomBarPosicion3.collectAsState()
    val bottomBarPosicion4 by viewModel.bottomBarPosicion4.collectAsState()
    val duracionMaximaAlarmaMin by viewModel.duracionMaximaAlarmaMin.collectAsState()

    var notificacionesPermitidas by remember { mutableStateOf(notificacionesPermitidas(context)) }
    var alarmasExactasPermitidas by remember { mutableStateOf(alarmasExactasPermitidas(context)) }
    var exentoDeOptimizacionBateria by remember { mutableStateOf(exentoDeOptimizacionBateria(context)) }
    // Estos permisos solo se cambian desde Ajustes del sistema, nunca desde acá — hay que
    // volver a revisarlos cada vez que el usuario regresa a esta pantalla (`ON_RESUME`), no
    // solo la primera vez que se compone.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificacionesPermitidas = notificacionesPermitidas(context)
                alarmasExactasPermitidas = alarmasExactasPermitidas(context)
                exentoDeOptimizacionBateria = exentoDeOptimizacionBateria(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Ajustes", style = MaterialTheme.typography.titleLarge)

        TarjetaSeccion(titulo = "🔔 Recordatorios y permisos") {
            if (!notificacionesPermitidas) {
                FilaPermiso(
                    emoji = "🔕",
                    texto = "Permitir notificaciones",
                    explicacion = "Sin este permiso, Lula no puede avisarte de tus recordatorios.",
                    onClick = { abrirAjustesDeNotificaciones(context) },
                )
            }
            if (!alarmasExactasPermitidas) {
                FilaPermiso(
                    emoji = "⏰",
                    texto = "Permitir alarmas exactas",
                    explicacion = "Sin esto, tus recordatorios pueden sonar varios minutos tarde.",
                    onClick = { abrirAjustesDeAlarmasExactas(context) },
                )
            }
            if (!exentoDeOptimizacionBateria) {
                FilaPermiso(
                    emoji = "🔋",
                    texto = "Permitir que Lula funcione siempre",
                    explicacion = "Algunos celulares (sobre todo Motorola, Xiaomi, Huawei) apagan Lula en " +
                        "segundo plano para ahorrar batería — eso puede cortar el sonido de una alarma a " +
                        "la mitad o que no llegue el recordatorio. Esto lo saca de esa lista.",
                    onClick = { abrirAjustesDeOptimizacionBateria(context) },
                )
            }
            SectionLinkRow(
                emoji = "🔊",
                color = LulaAsistenteContainerLight,
                texto = "Sonido de mis recordatorios",
                onClick = { abrirAjustesDeNotificaciones(context) },
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Vas a entrar a los ajustes del sistema Android, no de Lula — ahí puedes " +
                    "cambiar el sonido de cada canal (Silencioso/Sonido/Alarma). El texto que " +
                    "dice \"aproximadamente N notificaciones por día/semana\" también lo escribe " +
                    "Android: es su propio cálculo estimado según tu uso, no algo que Lula controle.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            Text(text = "⏱️ Silenciar alarma después de", modifier = Modifier.padding(top = 16.dp))
            Text(
                text = "El nivel Alarma suena en loop hasta que la apagas — elige un límite si " +
                    "prefieres que se corte sola.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(modifier = Modifier.padding(top = 8.dp)) {
                DURACIONES_MAXIMAS_ALARMA_MIN.forEach { minutos ->
                    FilterChip(
                        selected = duracionMaximaAlarmaMin == minutos,
                        onClick = { viewModel.setDuracionMaximaAlarmaMin(minutos) },
                        label = { Text(if (minutos == null) "Nunca" else "$minutos min") },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }

        TarjetaSeccion(titulo = "🗓️ Revisión y cierre del día") {
            Text(text = "Día en que se activa Revisión semanal", modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.padding(top = 8.dp)) {
                LETRAS_DIA_ISO.forEachIndexed { index, letra ->
                    val diaIso = index + 1
                    FilterChip(
                        selected = diaRevisionSemanal == diaIso,
                        onClick = { viewModel.setDiaRevisionSemanal(diaIso) },
                        label = { Text(letra) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
            Text(
                text = "Se activa los ${DateTimeUtils.nombreDiaIso(diaRevisionSemanal)} — antes de ese día " +
                    "no se puede completar, para no adelantarse a cómo termina la semana.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "🔥 Recordarme cerrar mi día", modifier = Modifier.weight(1f))
                Switch(
                    checked = horaRecordatorioCierreDia != null,
                    onCheckedChange = { activado -> viewModel.setHoraRecordatorioCierreDia(if (activado) "20:00" else null) },
                )
            }
            Text(
                text = "Un aviso a la hora que elijas si todavía no cerraste el día — para no perder " +
                    "la racha por olvido, sobre todo si tus hábitos no tienen recordatorio propio.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (horaRecordatorioCierreDia != null) {
                HoraSelector(
                    hora = horaRecordatorioCierreDia,
                    onHoraSeleccionada = { viewModel.setHoraRecordatorioCierreDia(it) },
                    etiqueta = "¿A qué hora te recuerdo?",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        TarjetaSeccion(titulo = "🔔 Recordarme revisar Lula") {
            Text(
                text = "Un aviso por franja del día si no entraste a revisar/marcar nada — útil " +
                    "sobre todo si tienes hábitos sin recordatorio propio. Independiente del de " +
                    "\"cerrar mi día\" de arriba.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            FilaRecordatorioFranja(
                etiqueta = "🌅 Por la mañana",
                hora = horaRecordatorioManana,
                horaPorDefecto = "09:00",
                onCambiar = { viewModel.setHoraRecordatorioFranja(MomentoDelDia.MANANA, it) },
            )
            FilaRecordatorioFranja(
                etiqueta = "🌤️ Por la tarde",
                hora = horaRecordatorioTarde,
                horaPorDefecto = "15:00",
                onCambiar = { viewModel.setHoraRecordatorioFranja(MomentoDelDia.TARDE, it) },
            )
            FilaRecordatorioFranja(
                etiqueta = "🌙 Por la noche",
                hora = horaRecordatorioNoche,
                horaPorDefecto = "20:00",
                onCambiar = { viewModel.setHoraRecordatorioFranja(MomentoDelDia.NOCHE, it) },
            )
        }

        TarjetaSeccion(titulo = "✅ Marcar en Hoy") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "🔔 Sonido al marcar un check en Hoy", modifier = Modifier.weight(1f))
                Switch(checked = sonidoCheckHabilitado, onCheckedChange = viewModel::setSonidoCheckHabilitado)
            }
        }

        TarjetaSeccion(titulo = "🧭 Personalizar mi navegación") {
            Text(
                text = "Elige qué va en cada posición de la barra inferior, a los costados del \"+\".",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            SelectorPosicionBottomBar(
                etiqueta = "Posición 2 (antes del \"+\")",
                seleccionId = bottomBarPosicion2,
                onSeleccionar = viewModel::setBottomBarPosicion2,
            )
            SelectorPosicionBottomBar(
                etiqueta = "Posición 3 (después del \"+\")",
                seleccionId = bottomBarPosicion3,
                onSeleccionar = viewModel::setBottomBarPosicion3,
            )
            SelectorPosicionBottomBar(
                etiqueta = "Posición 4 (después del \"+\")",
                seleccionId = bottomBarPosicion4,
                onSeleccionar = viewModel::setBottomBarPosicion4,
            )
        }
    }
}

@Composable
private fun FilaPermiso(emoji: String, texto: String, explicacion: String, onClick: () -> Unit) {
    SectionLinkRow(
        emoji = emoji,
        color = LulaAsistenteContainerLight,
        texto = texto,
        onClick = onClick,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(text = explicacion, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun FilaRecordatorioFranja(etiqueta: String, hora: String?, horaPorDefecto: String, onCambiar: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = etiqueta, modifier = Modifier.weight(1f))
        Switch(checked = hora != null, onCheckedChange = { activado -> onCambiar(if (activado) horaPorDefecto else null) })
    }
    if (hora != null) {
        HoraSelector(
            hora = hora,
            onHoraSeleccionada = onCambiar,
            etiqueta = "¿A qué hora te recuerdo?",
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorPosicionBottomBar(etiqueta: String, seleccionId: String, onSeleccionar: (String) -> Unit) {
    Text(text = etiqueta, modifier = Modifier.padding(top = 16.dp))
    FlowRow(modifier = Modifier.padding(top = 8.dp)) {
        OpcionBottomBar.entries.forEach { opcion ->
            FilterChip(
                selected = seleccionId == opcion.id,
                onClick = { onSeleccionar(opcion.id) },
                label = { Text("${opcion.emoji} ${opcion.etiqueta}") },
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
            )
        }
    }
}
