package com.aqpseller.lulaapp.features.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.SonidoCheckViewModel
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.core.utils.SonidoUtils
import com.aqpseller.lulaapp.domain.model.EstadoActividad

private enum class VistaTareas { LISTA, MATRIZ }

@Composable
fun TasksListScreen(
    onTareaClick: (String) -> Unit,
    onNuevaTarea: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksListViewModel = hiltViewModel(),
    sonidoCheckViewModel: SonidoCheckViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sonidoCheckHabilitado by sonidoCheckViewModel.habilitado.collectAsState()
    var vista by remember { mutableStateOf(VistaTareas.LISTA) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaTarea) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.tareas.isEmpty()) {
            EmptyState(
                emoji = "📝",
                titulo = "Todavía no tienes tareas.",
                subtitulo = "Toca + para crear la primera.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Tus tareas", style = MaterialTheme.typography.titleLarge)

            Row(modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = vista == VistaTareas.LISTA,
                    onClick = { vista = VistaTareas.LISTA },
                    label = { Text("Lista") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = vista == VistaTareas.MATRIZ,
                    onClick = { vista = VistaTareas.MATRIZ },
                    label = { Text("🗂️ Matriz") },
                )
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                if (vista == VistaTareas.LISTA) {
                    // Antes era una sola lista sin orden (más nueva creada primero) donde
                    // hechas y pendientes se mezclaban — a pedido del usuario, ahora las
                    // pendientes van primero (vencidas y con fecha próxima arriba) y las hechas
                    // quedan aparte, debajo de su propio encabezado. "Hechas" solo muestra lo
                    // completado HOY — sin este corte, esta sección crecía para siempre con años
                    // de tareas ya hechas, dejando de ser "lo vigente" (reportado por el usuario,
                    // ver `08-decisiones-tecnicas.md`); lo completado otro día sigue viéndose en
                    // Calendario, en el día en que se hizo.
                    val pendientes = uiState.tareas.pendientesOrdenadas()
                    val hoy = DateTimeUtils.hoy()
                    val hechas = uiState.tareas.filter {
                        it.estado == EstadoActividad.CONFIRMADO &&
                            it.fechaCompletado?.let { f -> DateTimeUtils.epochMillisToLocalDate(f) == hoy } == true
                    }
                    if (pendientes.isNotEmpty()) {
                        item {
                            Text(text = "PENDIENTES", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        items(pendientes, key = { it.id }) { tarea ->
                            FilaTarea(tarea, onTareaClick, sonidoCheckHabilitado, viewModel)
                            HorizontalDivider()
                        }
                    }
                    if (hechas.isNotEmpty()) {
                        item {
                            Text(text = "✅ HECHAS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                        }
                        items(hechas, key = { it.id }) { tarea ->
                            FilaTarea(tarea, onTareaClick, sonidoCheckHabilitado, viewModel)
                            HorizontalDivider()
                        }
                    }
                } else {
                    // La Matriz es para decidir qué hacer — una tarea ya completada no aporta
                    // nada ahí (y sin este filtro se quedaba para siempre en su cuadrante,
                    // creciendo sin límite). Ver `08-decisiones-tecnicas.md`.
                    val pendientesMatriz = uiState.tareas.filter { it.estado != EstadoActividad.CONFIRMADO }
                    seccionMatriz(
                        titulo = "🔥 HACER (urgente + importante)",
                        tareas = pendientesMatriz.filter { it.urgente && it.importante },
                        onTareaClick = onTareaClick,
                        sonidoCheckHabilitado = sonidoCheckHabilitado,
                        viewModel = viewModel,
                    )
                    seccionMatriz(
                        titulo = "🗓️ PROGRAMAR (importante, no urgente)",
                        tareas = pendientesMatriz.filter { !it.urgente && it.importante },
                        onTareaClick = onTareaClick,
                        sonidoCheckHabilitado = sonidoCheckHabilitado,
                        viewModel = viewModel,
                    )
                    seccionMatriz(
                        titulo = "🤝 DELEGAR (urgente, no importante)",
                        tareas = pendientesMatriz.filter { it.urgente && !it.importante },
                        onTareaClick = onTareaClick,
                        sonidoCheckHabilitado = sonidoCheckHabilitado,
                        viewModel = viewModel,
                    )
                    seccionMatriz(
                        titulo = "🗑️ POSPONER (ninguna)",
                        tareas = pendientesMatriz.filter { !it.urgente && !it.importante },
                        onTareaClick = onTareaClick,
                        sonidoCheckHabilitado = sonidoCheckHabilitado,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

/** Vencidas y con fecha próxima primero; sin fecha límite, al final del grupo de pendientes. */
private fun List<TareaListItemUi>.pendientesOrdenadas(): List<TareaListItemUi> =
    filter { it.estado != EstadoActividad.CONFIRMADO }
        .sortedWith(compareBy({ it.fechaLimite == null }, { it.fechaLimite ?: Long.MAX_VALUE }))

private fun LazyListScope.seccionMatriz(
    titulo: String,
    tareas: List<TareaListItemUi>,
    onTareaClick: (String) -> Unit,
    sonidoCheckHabilitado: Boolean,
    viewModel: TasksListViewModel,
) {
    if (tareas.isEmpty()) return
    item {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
    }
    items(tareas, key = { it.id }) { tarea ->
        FilaTarea(tarea, onTareaClick, sonidoCheckHabilitado, viewModel)
        HorizontalDivider()
    }
}

@Composable
private fun FilaTarea(
    tarea: TareaListItemUi,
    onTareaClick: (String) -> Unit,
    sonidoCheckHabilitado: Boolean,
    viewModel: TasksListViewModel,
) {
    // El check y el nombre tienen su propio toque por separado — antes el `Row` entero
    // (incluido el checkbox) era clickeable hacia el detalle, y los dos gestos superpuestos
    // hacían que marcar el check no se viera reflejado.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val completada = tarea.estado == EstadoActividad.CONFIRMADO
        Checkbox(
            checked = completada,
            onCheckedChange = {
                viewModel.alternarCompletada(tarea.id, tarea.estado)
                if (sonidoCheckHabilitado) SonidoUtils.reproducirCheck()
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onTareaClick(tarea.id) },
        ) {
            Text(
                text = tarea.nombre,
                textDecoration = if (completada) TextDecoration.LineThrough else null,
                color = if (completada) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            tarea.fechaLimite?.let { fechaLimite ->
                val vencida = !completada && fechaLimite < DateTimeUtils.inicioDeHoyEpochMillis()
                Text(
                    text = "📅 ${DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaLimite))}" + if (vencida) " ⚠️ vencida" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
