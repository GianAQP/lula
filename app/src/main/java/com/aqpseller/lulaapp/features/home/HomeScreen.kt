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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerDark
import com.aqpseller.lulaapp.ui.theme.LulaPrimaryContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaTareaContainerLight
import com.aqpseller.lulaapp.ui.theme.lulaCardColors
import com.aqpseller.lulaapp.ui.theme.lulaContainerColor
import com.aqpseller.lulaapp.ui.theme.lulaContentColorSobreContainer

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
            seccionMetas(uiState.metasEnProgreso, onVerMetaDetail)

            seccionAgenda("🎉 FECHAS IMPORTANTES DE HOY", uiState.fechasImportantesDeHoy, onVerFechaImportante)
            seccionAgenda("📅 CITAS DE HOY", uiState.citasDeHoy, onVerCita)

            seccionRevisionesHabito(uiState.revisionesHabitoPendientes, viewModel)

            // Lo pendiente se muestra primero y agrupado — lo ya hecho se saca de acá para no
            // competir visualmente, pero sigue disponible (nunca se borra ni se castiga, ver
            // `Plan/CLAUDE.md`) en "✅ Ya hechos hoy" más abajo, donde también se puede deshacer.
            seccionActividades("POR LA MAÑANA", uiState.actividadesManana.pendientes(), viewModel, uiState.sonidoCheckHabilitado)
            seccionActividades("POR LA TARDE", uiState.actividadesTarde.pendientes(), viewModel, uiState.sonidoCheckHabilitado)
            seccionActividades("POR LA NOCHE", uiState.actividadesNoche.pendientes(), viewModel, uiState.sonidoCheckHabilitado)
            seccionActividades("TAREAS DE HOY", uiState.tareasDeHoy.pendientes(), viewModel, uiState.sonidoCheckHabilitado)
            seccionMedicamentos(uiState.medicamentosDeHoy.conSoloTomasPendientes(), viewModel)

            item {
                val completados = uiState.actividadesManana + uiState.actividadesTarde +
                    uiState.actividadesNoche + uiState.tareasDeHoy
                val tomasResueltas = uiState.medicamentosDeHoy.flatMap { medicamento ->
                    medicamento.tomas.filter { it.estado != EstadoActividad.SIN_CONFIRMAR }.map { medicamento to it }
                }
                SeccionYaHechosHoy(
                    completados = completados.filter { it.estado == EstadoActividad.CONFIRMADO },
                    tomasResueltas = tomasResueltas,
                    viewModel = viewModel,
                    sonidoCheckHabilitado = uiState.sonidoCheckHabilitado,
                )
            }

            item {
                SectionLinkRow(
                    emoji = "📝",
                    color = LulaTareaContainerLight,
                    texto = "Ver todas mis tareas",
                    onClick = onVerTareas,
                    modifier = Modifier.padding(top = 4.dp),
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
 * checkbox acá (a diferencia de Hábitos/Tareas, no se "completan" desde Hoy). */
private fun androidx.compose.foundation.lazy.LazyListScope.seccionAgenda(
    titulo: String,
    items: List<ItemAgenda>,
    onItemClick: (String) -> Unit,
) {
    if (items.isEmpty()) return
    item {
        Text(text = titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    }
    val horaActual = DateTimeUtils.horaHHmm(DateTimeUtils.ahoraEpochMillis())
    items(items, key = { it.actividadId }) { item ->
        // Ya pasó su hora y sigue sin marcar — se resalta para que llame la atención, en vez de
        // quedar igual que algo que todavía falta por venir (a pedido del usuario).
        val vencida = item.estado == EstadoActividad.SIN_CONFIRMAR && item.horario != null && item.horario < horaActual
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(item.actividadId) }
                .padding(vertical = 6.dp),
        ) {
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
    }
}

/**
 * Antes las Metas solo aparecían como un enlace "Ver mis metas" en Hoy, sin ningún progreso
 * visible — a pedido del usuario, ahora se ven acá con su barra, igual que Notas/Diario ya
 * hacían con sus propios "¿hay algo?" — ver `Plan/08-decisiones-tecnicas.md`.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.seccionMetas(
    metas: List<com.aqpseller.lulaapp.domain.usecase.meta.MetaConProgreso>,
    onMetaClick: (String) -> Unit,
) {
    if (metas.isEmpty()) return
    item {
        Text(text = "🎯 TUS METAS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    }
    items(metas, key = { it.meta.id }) { metaConProgreso ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMetaClick(metaConProgreso.meta.id) }
                .padding(vertical = 6.dp),
        ) {
            Text(text = metaConProgreso.meta.nombre, style = MaterialTheme.typography.bodyMedium)
            LulaProgressBar(progreso = metaConProgreso.fraccion, modifier = Modifier.padding(top = 4.dp))
            Text(
                text = "${metaConProgreso.progreso.toInt()} de ${metaConProgreso.meta.valorObjetivo.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
            // Solo se resalta en los últimos 7 días (o ya vencida) — la fecha límite no debe
            // sentirse como presión constante desde el día 1, ver `08-decisiones-tecnicas.md`.
            if (metaConProgreso.esUrgente) {
                val dias = metaConProgreso.diasRestantes ?: 0
                Text(
                    text = if (dias >= 0) "⏳ Faltan $dias día(s)" else "⏳ Venció hace ${-dias} día(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 2.dp),
                )
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

private fun androidx.compose.foundation.lazy.LazyListScope.seccionActividades(
    titulo: String,
    actividades: List<ActividadUi>,
    viewModel: HomeViewModel,
    sonidoCheckHabilitado: Boolean,
) {
    if (actividades.isEmpty()) return
    item {
        Text(text = titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    }
    val horaActual = DateTimeUtils.horaHHmm(DateTimeUtils.ahoraEpochMillis())
    items(actividades, key = { it.id }) { actividad ->
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
                .padding(vertical = 4.dp),
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
 * vuelve arriba, a su sección de pendientes.
 */
@Composable
private fun SeccionYaHechosHoy(
    completados: List<ActividadUi>,
    tomasResueltas: List<Pair<MedicamentoDeHoy, TomaDeHoy>>,
    viewModel: HomeViewModel,
    sonidoCheckHabilitado: Boolean,
) {
    if (completados.isEmpty() && tomasResueltas.isEmpty()) return
    var expandido by remember { mutableStateOf(false) }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        Text(text = if (expandido) "▲" else "▼")
    }

    if (expandido) {
        completados.forEach { actividad ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = true,
                    onCheckedChange = {
                        viewModel.marcarActividad(actividad.id, EstadoActividad.SIN_CONFIRMAR)
                        if (sonidoCheckHabilitado) SonidoUtils.reproducirCheck()
                    },
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

private fun androidx.compose.foundation.lazy.LazyListScope.seccionMedicamentos(
    medicamentos: List<MedicamentoDeHoy>,
    viewModel: HomeViewModel,
) {
    if (medicamentos.isEmpty()) return
    item {
        Text(text = "MEDICAMENTOS DE HOY", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    }
    items(medicamentos, key = { it.actividadId }) { medicamento ->
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
