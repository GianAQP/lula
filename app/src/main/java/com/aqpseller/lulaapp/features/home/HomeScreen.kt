package com.aqpseller.lulaapp.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.LulaProgressBar
import com.aqpseller.lulaapp.core.ui.SectionLinkRow
import com.aqpseller.lulaapp.core.ui.TarjetaSeccion
import com.aqpseller.lulaapp.core.ui.TomaAccionRow
import com.aqpseller.lulaapp.core.ui.emojiEstadoActividad
import com.aqpseller.lulaapp.core.ui.emojiTipoActividad
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.SonidoUtils
import com.aqpseller.lulaapp.core.utils.abrirAjustesDeNotificaciones
import com.aqpseller.lulaapp.core.utils.abrirAjustesDeOptimizacionBateria
import com.aqpseller.lulaapp.core.utils.exentoDeOptimizacionBateria
import com.aqpseller.lulaapp.core.utils.notificacionesPermitidas
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.ItemAgenda
import com.aqpseller.lulaapp.domain.model.MedicamentoDeHoy
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.domain.model.TomaDeHoy
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaFamiliaContainerDark
import com.aqpseller.lulaapp.ui.theme.LulaFamiliaContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaHabito
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerDark
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaTareaContainerLight
import com.aqpseller.lulaapp.ui.theme.lulaCardColors
import com.aqpseller.lulaapp.ui.theme.lulaContainerColor
import com.aqpseller.lulaapp.ui.theme.lulaContentColorSobreContainer

/** Verde de confirmación (mismo verde de "hábitos/crecimiento" de la paleta) para todo check
 * marcado — antes usaba el violeta primario por defecto de Material y se confundía con el resto
 * de la UI en vez de leerse como "listo". A pedido del usuario. Ver `08-decisiones-tecnicas.md`. */
@Composable
private fun coloresCheckMarcar() = CheckboxDefaults.colors(checkedColor = LulaHabito, checkmarkColor = Color.White)

@Composable
fun HomeScreen(
    onCerrarDia: () -> Unit,
    onAgregarAlgo: () -> Unit,
    onVerTareas: () -> Unit,
    onVerMetas: () -> Unit,
    onVerRutinas: () -> Unit,
    onVerProgreso: () -> Unit,
    onVerListas: () -> Unit,
    onVerSalud: () -> Unit,
    onVerFechasImportantes: () -> Unit,
    onVerNotas: () -> Unit,
    onVerCalendario: () -> Unit,
    onVerFamilia: () -> Unit,
    onVerCita: (String) -> Unit,
    onVerFechaImportante: (String) -> Unit,
    onVerMetaDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // Android nunca vuelve a mostrar el diálogo de sistema después de una negación — sin este
    // aviso, un recordatorio programado "suena" en el sentido de que la alarma sí se dispara,
    // pero la notificación se descarta en silencio, sin ningún rastro visible en la app (bug
    // real reportado por el usuario, ver `08-decisiones-tecnicas.md`).
    var notificacionesPermitidas by remember { mutableStateOf(notificacionesPermitidas(context)) }
    // Motivo real, ya diagnosticado con el usuario, de "sonó un segundo y se cortó" en el nivel
    // Alarma: el fabricante mata a Lula en segundo plano si no tiene esta excepción. Antes solo
    // se veía si el usuario entraba por su cuenta a Ajustes — igual que con el permiso de
    // notificaciones, ahora se avisa acá sin que tenga que ir a buscarlo. Ver
    // `Plan/08-decisiones-tecnicas.md`.
    var exentoDeOptimizacionBateria by remember { mutableStateOf(exentoDeOptimizacionBateria(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificacionesPermitidas = notificacionesPermitidas(context)
                exentoDeOptimizacionBateria = exentoDeOptimizacionBateria(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.cargando) {
            return@Column
        }

        if (!notificacionesPermitidas) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable { abrirAjustesDeNotificaciones(context) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🔕 Sin permiso de notificaciones — tus recordatorios no van a avisarte",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "Activar →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        if (!exentoDeOptimizacionBateria) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable { abrirAjustesDeOptimizacionBateria(context) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🔋 Tu celular puede apagar Lula en segundo plano — tus alarmas pueden sonar cortadas o no sonar",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(text = "Activar →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        if (uiState.pendientesEnFamilia > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(lulaContainerColor(LulaFamiliaContainerLight, LulaFamiliaContainerDark))
                    .clickable(onClick = onVerFamilia)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "👨‍👩‍👧 Tienes ${uiState.pendientesEnFamilia} pendiente(s) en tu espacio Familia",
                    style = MaterialTheme.typography.labelLarge,
                    color = lulaContentColorSobreContainer(),
                    modifier = Modifier.weight(1f),
                )
                Text(text = "Ver →", style = MaterialTheme.typography.labelLarge, color = lulaContentColorSobreContainer())
            }
        }

        // Motivador, no de castigo — a propósito no usa el color de error de los banners de
        // arriba. La racha 🔥 ya no baja sola (ver `08-decisiones-tecnicas.md`), esto solo
        // invita a cerrar el día que se quedó pendiente para que siga sumando.
        if (uiState.diaAnteriorSinCerrar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(lulaContainerColor(LulaPrimaryContainerLight, LulaPrimaryContainerDark))
                    .clickable(onClick = onVerCalendario)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🔥 Ayer se quedó sin cerrar — ciérralo desde el calendario y tu racha sigue sumando",
                    style = MaterialTheme.typography.labelLarge,
                    color = lulaContentColorSobreContainer(),
                    modifier = Modifier.weight(1f),
                )
                Text(text = "Cerrar →", style = MaterialTheme.typography.labelLarge, color = lulaContentColorSobreContainer())
            }
        }

        if (!uiState.hayAlgoParaHoy) {
            EmptyState(
                emoji = "🌱",
                titulo = "Todavía no tienes actividades para hoy.",
                textoBoton = "Agregar algo para hoy",
                onBotonClick = onAgregarAlgo,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onVerCalendario, modifier = Modifier.fillMaxWidth()) {
                Text("📅 Ver calendario")
            }
            return@Column
        }

        // Listas pendientes calculadas una sola vez acá (antes se repetían inline) — además de
        // pintar cada sección, sirven para decidir si la tarjeta "✅ Para marcar hoy" tiene algo
        // que mostrar, ya que ahora todas viven agrupadas dentro de una sola tarjeta en vez de
        // aparecer sueltas una tras otra.
        val actividadesMananaPendientes = uiState.actividadesManana.pendientes()
        val actividadesTardePendientes = uiState.actividadesTarde.pendientes()
        val actividadesNochePendientes = uiState.actividadesNoche.pendientes()
        val tareasPendientes = uiState.tareasDeHoy.pendientes()
        val medicamentosPendientes = uiState.medicamentosDeHoy.conSoloTomasPendientes()
        val hayParaMarcar = actividadesMananaPendientes.isNotEmpty() || actividadesTardePendientes.isNotEmpty() ||
            actividadesNochePendientes.isNotEmpty() || tareasPendientes.isNotEmpty() || medicamentosPendientes.isNotEmpty()
        val hayAgendaOMetas = uiState.metasEnProgreso.isNotEmpty() || uiState.fechasImportantesDeHoy.isNotEmpty() ||
            uiState.citasDeHoy.isNotEmpty()
        val completados = uiState.actividadesManana + uiState.actividadesTarde +
            uiState.actividadesNoche + uiState.tareasDeHoy
        val completadosConfirmados = completados.filter { it.estado == EstadoActividad.CONFIRMADO }
        val tomasResueltas = uiState.medicamentosDeHoy.flatMap { medicamento ->
            medicamento.tomas.filter { it.estado != EstadoActividad.SIN_CONFIRMAR }.map { medicamento to it }
        }
        val hayYaHechos = completadosConfirmados.isNotEmpty() || tomasResueltas.isNotEmpty()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text(text = "Progreso de hoy", modifier = Modifier.padding(top = 8.dp))
                val progreso = if (uiState.totalActividades > 0) {
                    uiState.completadas.toFloat() / uiState.totalActividades
                } else {
                    0f
                }
                LulaProgressBar(progreso = progreso, modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = "${uiState.completadas} de ${uiState.totalActividades}",
                    modifier = Modifier.padding(top = 4.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            seccionHitosMeta(uiState.hitosMetaPendientes, viewModel)

            // Todo lo que solo se abre para ver/editar (Metas, Fechas importantes, Citas) va
            // junto en una sola tarjeta — separado a propósito de lo que se marca con checkbox,
            // a pedido del usuario mostrando ejemplos de otras apps.
            if (hayAgendaOMetas) {
                item {
                    TarjetaSeccion(titulo = "📌 Metas y agenda de hoy") {
                        MetasSeccion(uiState.metasEnProgreso, onVerMetaDetail)
                        AgendaSeccion("🎉 Fechas importantes", uiState.fechasImportantesDeHoy, onVerFechaImportante)
                        AgendaSeccion("📅 Citas", uiState.citasDeHoy, onVerCita)
                    }
                }
            }

            seccionRevisionesHabito(uiState.revisionesHabitoPendientes, viewModel)

            // Todo lo que se marca con checkbox (Hábitos/Tareas/Medicamentos pendientes) va junto
            // en su propia tarjeta — lo ya hecho se saca de acá para no competir visualmente,
            // pero sigue disponible (nunca se borra ni se castiga, ver `Plan/CLAUDE.md`) en
            // "✅ Ya hechos hoy", su propia tarjeta más abajo.
            if (hayParaMarcar) {
                item {
                    TarjetaSeccion(titulo = "✅ Para marcar hoy") {
                        ActividadesSeccion("Por la mañana", actividadesMananaPendientes, viewModel, uiState.sonidoCheckHabilitado)
                        ActividadesSeccion("Por la tarde", actividadesTardePendientes, viewModel, uiState.sonidoCheckHabilitado)
                        ActividadesSeccion("Por la noche", actividadesNochePendientes, viewModel, uiState.sonidoCheckHabilitado)
                        ActividadesSeccion("Tareas de hoy", tareasPendientes, viewModel, uiState.sonidoCheckHabilitado)
                        MedicamentosSeccion(medicamentosPendientes, viewModel)
                    }
                }
            }

            if (hayYaHechos) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SeccionYaHechosHoy(
                                completados = completadosConfirmados,
                                tomasResueltas = tomasResueltas,
                                viewModel = viewModel,
                                sonidoCheckHabilitado = uiState.sonidoCheckHabilitado,
                            )
                        }
                    }
                }
            }

            item {
                TarjetaSeccion(titulo = "🔎 Explorar más") {
                    SectionLinkRow(
                        emoji = "📝",
                        color = LulaTareaContainerLight,
                        texto = "Ver todas mis tareas",
                        onClick = onVerTareas,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    SectionLinkRow(
                        emoji = "📅",
                        color = LulaPrimaryContainerLight,
                        texto = "Ver calendario",
                        onClick = onVerCalendario,
                    )
                    if (uiState.hayMedicamentosOCitas) {
                        SectionLinkRow(
                            emoji = "💊",
                            color = LulaAsistenteContainerLight,
                            texto = "Ver mi salud",
                            onClick = onVerSalud,
                        )
                    }
                    // "Ver mis metas"/"Ver mis rutinas" solo aparecen si ya hay al menos una — se
                    // descubren desde el menú "+" (que sí siempre está disponible), no se muestran
                    // enlaces a listas vacías (a pedido del usuario).
                    if (uiState.hayMetas) {
                        SectionLinkRow(
                            emoji = "🎯",
                            color = LulaPrimaryContainerLight,
                            texto = "Ver mis metas",
                            onClick = onVerMetas,
                        )
                    }
                    if (uiState.hayRutinas) {
                        SectionLinkRow(
                            emoji = "🧩",
                            color = LulaPrimaryContainerLight,
                            texto = "Ver mis rutinas",
                            onClick = onVerRutinas,
                        )
                    }
                    if (uiState.hayListas) {
                        SectionLinkRow(
                            emoji = "📋",
                            color = LulaPrimaryContainerLight,
                            texto = "Ver mis listas",
                            onClick = onVerListas,
                        )
                    }
                    if (uiState.hayFechasImportantes) {
                        SectionLinkRow(
                            emoji = "🎉",
                            color = LulaPrimaryContainerLight,
                            texto = "Ver mis fechas importantes",
                            onClick = onVerFechasImportantes,
                        )
                    }
                    if (uiState.hayNotas) {
                        SectionLinkRow(
                            emoji = "📝",
                            color = LulaPrimaryContainerLight,
                            texto = "Ver mis notas",
                            onClick = onVerNotas,
                        )
                    }
                    SectionLinkRow(
                        emoji = "📊",
                        color = LulaPrimaryContainerLight,
                        texto = "Ver mi progreso",
                        onClick = onVerProgreso,
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(text = "Resumen rápido", style = MaterialTheme.typography.titleSmall)
                Text(text = "💰 Gastos hoy: S/ ${"%.2f".format(uiState.gastosHoyTotal)}")
            }
        }

        Button(
            onClick = onCerrarDia,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(if (uiState.diaYaCerrado) "Actualizar cierre del día" else "Cerrar mi día")
        }
    }
}

/**
 * Tarjeta "¿Aumentamos?" de `02-pantallas.md` — aparece cuando toca revisar un Hábito
 * progresivo. Lula solo pregunta, nunca decide sola.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.seccionRevisionesHabito(
    revisiones: List<RevisionHabitoUi>,
    viewModel: HomeViewModel,
) {
    items(revisiones, key = { it.actividadId }) { revision ->
        Card(
            colors = lulaCardColors(LulaPrimaryContainerLight, LulaPrimaryContainerDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Completaste \"${revision.nombre}\" ${revision.diasCumplidos} de ${revision.diasTotales} días.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Actualmente haces ${revision.duracionActualMin} min. ¿Aumentamos?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    Button(
                        onClick = { viewModel.responderRevisionSubir(revision) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Subir a ${(revision.duracionActualMin + revision.incrementoMin).coerceAtMost(revision.duracionObjetivoMin)} min")
                    }
                    OutlinedButton(
                        onClick = { viewModel.responderRevisionMantener(revision) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Mantener")
                    }
                    TextButton(onClick = { viewModel.responderRevisionRecordarDespues(revision) }) {
                        Text("Después")
                    }
                }
            }
        }
    }
}

/** Fila de solo lectura para Citas/Fechas importantes de hoy — tocar lleva al detalle, sin
 * checkbox acá (a diferencia de Hábitos/Tareas, no se "completan" desde Hoy). Antes vivía
 * directamente en el `LazyColumn`; ahora es un composable normal porque va dentro de la tarjeta
 * compartida "📌 Metas y agenda de hoy". */
@Composable
private fun AgendaSeccion(
    titulo: String,
    items: List<ItemAgenda>,
    onItemClick: (String) -> Unit,
) {
    if (items.isEmpty()) return
    Text(text = titulo, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
    val horaActual = DateTimeUtils.horaHHmm(DateTimeUtils.ahoraEpochMillis())
    items.forEach { item ->
        // Ya pasó su hora y sigue sin marcar — se resalta con fondo (no solo texto rojo) para
        // que sea fácil de ubicar de un vistazo cuál toca resolver ahora, a pedido del usuario.
        val vencida = item.estado == EstadoActividad.SIN_CONFIRMAR && item.horario != null && item.horario < horaActual
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(item.actividadId) }
                .then(if (vencida) Modifier.background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)) else Modifier)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${emojiEstadoActividad(item.estado)} ${item.nombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (vencida) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
                val detalle = listOfNotNull(item.horario, item.subtitulo).joinToString(" · ")
                if (detalle.isNotEmpty()) {
                    Text(
                        text = detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (vencida) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
            }
            // Solo se abre para ver/editar, no se marca acá — la flecha lo distingue de un
            // vistazo de las filas con checkbox (mismo lenguaje que `SectionLinkRow`), a pedido
            // del usuario. Ver `Plan/08-decisiones-tecnicas.md`.
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * Antes las Metas solo aparecían como un enlace "Ver mis metas" en Hoy, sin ningún progreso
 * visible — a pedido del usuario, ahora se ven acá con su barra, igual que Notas/Diario ya
 * hacían con sus propios "¿hay algo?" — ver `Plan/08-decisiones-tecnicas.md`. Composable normal
 * (antes vivía suelta en el `LazyColumn`) porque ahora va dentro de la tarjeta compartida
 * "📌 Metas y agenda de hoy".
 */
@Composable
private fun MetasSeccion(
    metas: List<com.aqpseller.lulaapp.domain.usecase.meta.MetaConProgreso>,
    onMetaClick: (String) -> Unit,
) {
    if (metas.isEmpty()) return
    Text(text = "🎯 Tus metas por vencer", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
    // Mismo estilo compacto de "Ver mis metas" (nombre a la izquierda, contador a la derecha,
    // sin barra de progreso) — antes tenía su propia barra, distinta a la de la lista completa,
    // y el usuario pidió que se vea "bonito" acá también. El aviso de días restantes ahora es un
    // atajo directo a reprogramar (aplazar), no solo texto — ver `Plan/08-decisiones-tecnicas.md`.
    metas.forEach { metaConProgreso ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMetaClick(metaConProgreso.meta.id) }
                .padding(vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = metaConProgreso.meta.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(end = 8.dp))
                Text(
                    text = "(${metaConProgreso.progreso.toInt()}/${metaConProgreso.meta.valorObjetivo.toInt()})",
                    style = MaterialTheme.typography.titleSmall,
                )
                // Se abre para ver el detalle, no se marca acá — misma flecha que Citas/Fechas
                // importantes, para que se lea igual de un vistazo. Ver `08-decisiones-tecnicas.md`.
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            // Solo se resalta en los últimos 7 días (o ya vencida) — la fecha límite no debe
            // sentirse como presión constante desde el día 1, ver `08-decisiones-tecnicas.md`.
            if (metaConProgreso.esUrgente) {
                val dias = metaConProgreso.diasRestantes ?: 0
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = if (dias >= 0) "⏳ Faltan $dias día(s)" else "⏳ Venció hace ${-dias} día(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onMetaClick(metaConProgreso.meta.id) }) {
                        Text("🔜 Reprogramar")
                    }
                }
            }
        }
    }
}

/**
 * Reconocimiento breve al cruzar 25/50/75/100% de una Meta — chico y positivo a propósito, no
 * una alarma ni un logro con sonido; coherente con que en Lula ningún intento se castiga, así
 * que tampoco hace falta exagerar el premio. Se marca "vista" al tocar el botón y no vuelve a
 * aparecer para ese mismo hito (`Meta.ultimoHitoCelebrado`).
 */
private fun androidx.compose.foundation.lazy.LazyListScope.seccionHitosMeta(
    hitos: List<com.aqpseller.lulaapp.domain.usecase.meta.MetaConProgreso>,
    viewModel: HomeViewModel,
) {
    items(hitos, key = { "hito:${it.meta.id}" }) { metaConProgreso ->
        Card(
            colors = lulaCardColors(LulaPrimaryContainerLight, LulaPrimaryContainerDark),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (metaConProgreso.hitoActual >= 100) {
                        "🎉 ¡Completaste tu meta \"${metaConProgreso.meta.nombre}\"!"
                    } else {
                        "🎉 ¡Vas al ${metaConProgreso.hitoActual}% de \"${metaConProgreso.meta.nombre}\"!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { viewModel.marcarHitoMetaCelebrado(metaConProgreso) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Genial")
                }
            }
        }
    }
}

/** Composable normal (antes vivía suelta en el `LazyColumn`) porque ahora va dentro de la
 * tarjeta compartida "✅ Para marcar hoy". */
@Composable
private fun ActividadesSeccion(
    titulo: String,
    actividades: List<ActividadUi>,
    viewModel: HomeViewModel,
    sonidoCheckHabilitado: Boolean,
) {
    if (actividades.isEmpty()) return
    Text(text = titulo, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
    val horaActual = DateTimeUtils.horaHHmm(DateTimeUtils.ahoraEpochMillis())
    actividades.forEach { actividad ->
        // Ya pasó su hora y sigue sin marcar — mismo criterio que Cita/Fecha importante/Medicamento.
        // Una Tarea, además, cuenta como vencida si su fecha límite (no solo la hora) ya pasó —
        // antes solo se miraba `horaRecordatorio`, así que una tarea vencida de días atrás sin
        // hora de aviso configurada nunca se pintaba en rojo (ver `08-decisiones-tecnicas.md`).
        val vencidaPorFecha = actividad.tipo == TipoActividad.TAREA &&
            actividad.fechaLimite != null && actividad.fechaLimite < DateTimeUtils.inicioDeHoyEpochMillis()
        val vencida = actividad.estado == EstadoActividad.SIN_CONFIRMAR &&
            (vencidaPorFecha || (actividad.horaRecordatorio != null && actividad.horaRecordatorio < horaActual))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (vencida) Modifier.background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)) else Modifier)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = actividad.estado == EstadoActividad.CONFIRMADO,
                onCheckedChange = { marcado ->
                    viewModel.marcarActividad(
                        actividad.id,
                        if (marcado) EstadoActividad.CONFIRMADO else EstadoActividad.SIN_CONFIRMAR,
                    )
                    if (sonidoCheckHabilitado) SonidoUtils.reproducirCheck()
                },
                colors = coloresCheckMarcar(),
            )
            Text(
                text = "${emojiTipoActividad(actividad.tipo)} ${actividad.nombre}" + if (vencida) " ⚠️" else "",
                textDecoration = if (actividad.estado == EstadoActividad.CONFIRMADO) TextDecoration.LineThrough else null,
                color = if (vencida) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        }
    }
}

private fun List<ActividadUi>.pendientes(): List<ActividadUi> = filter { it.estado != EstadoActividad.CONFIRMADO }

private fun List<MedicamentoDeHoy>.conSoloTomasPendientes(): List<MedicamentoDeHoy> = mapNotNull { medicamento ->
    val pendientes = medicamento.tomas.filter { it.estado == EstadoActividad.SIN_CONFIRMAR }
    if (pendientes.isEmpty()) null else medicamento.copy(tomas = pendientes)
}

/**
 * Todo lo ya completado hoy (hábitos, tareas y tomas de medicamento resueltas), junto en un
 * solo lugar colapsado por defecto — a pedido del usuario, para que lo pendiente resalte y lo
 * hecho no estorbe, sin ocultarlo para siempre: se puede expandir y desmarcar, y al desmarcar
 * vuelve arriba, a su sección de pendientes. Ahora vive dentro de su propia tarjeta redondeada
 * (antes solo tenía un divisor arriba) — el divisor se sacó porque ya no hace falta separar de
 * nada, la tarjeta misma es la separación.
 */
@Composable
private fun SeccionYaHechosHoy(
    completados: List<ActividadUi>,
    tomasResueltas: List<Pair<MedicamentoDeHoy, TomaDeHoy>>,
    viewModel: HomeViewModel,
    sonidoCheckHabilitado: Boolean,
) {
    var expandido by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A propósito no es el mismo número que "Progreso de hoy" — acá también suman las tomas
        // de Medicamento resueltas (confirmadas u omitidas), que "Progreso de hoy"/"Cerrar día"
        // no cuentan (se trackean aparte, por toma). El desglose evita que parezca un tercer
        // total contradictorio. Ver `Plan/08-decisiones-tecnicas.md`.
        val etiquetaConteo = if (tomasResueltas.isEmpty()) {
            "${completados.size}"
        } else {
            "${completados.size} + ${tomasResueltas.size} tomas"
        }
        Text(
            text = "✅ Ya hechos hoy ($etiquetaConteo)",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Text(text = if (expandido) "▲" else "▼")
    }

    if (expandido) {
        completados.forEach { actividad ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = true,
                    onCheckedChange = {
                        viewModel.marcarActividad(actividad.id, EstadoActividad.SIN_CONFIRMAR)
                        if (sonidoCheckHabilitado) SonidoUtils.reproducirCheck()
                    },
                    colors = coloresCheckMarcar(),
                )
                Text(
                    text = "${emojiTipoActividad(actividad.tipo)} ${actividad.nombre}",
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
        tomasResueltas.forEach { (medicamento, toma) ->
            TomaAccionRow(
                nombreMedicamento = medicamento.nombre,
                horario = toma.horario,
                instruccion = toma.instruccion,
                estado = toma.estado,
                onMarcar = { estado -> viewModel.marcarToma(medicamento.actividadId, toma.horario, estado) },
            )
        }
    }
}

/** Composable normal (antes vivía suelta en el `LazyColumn`) porque ahora va dentro de la
 * tarjeta compartida "✅ Para marcar hoy". */
@Composable
private fun MedicamentosSeccion(
    medicamentos: List<MedicamentoDeHoy>,
    viewModel: HomeViewModel,
) {
    if (medicamentos.isEmpty()) return
    Text(text = "Medicamentos de hoy", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
    medicamentos.forEach { medicamento ->
        Column {
            medicamento.tomas.forEach { toma ->
                TomaAccionRow(
                    nombreMedicamento = medicamento.nombre,
                    horario = toma.horario,
                    instruccion = toma.instruccion,
                    estado = toma.estado,
                    onMarcar = { estado -> viewModel.marcarToma(medicamento.actividadId, toma.horario, estado) },
                )
            }
        }
    }
}
