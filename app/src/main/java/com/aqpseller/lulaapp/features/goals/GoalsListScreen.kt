package com.aqpseller.lulaapp.features.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.CategoriaMeta

private fun etiquetaCategoria(categoria: CategoriaMeta?): String = when (categoria) {
    CategoriaMeta.HACER -> "🎯 Qué quiero hacer"
    CategoriaMeta.SER -> "🎯 Qué quiero ser"
    CategoriaMeta.VER -> "🎯 Qué quiero ver"
    CategoriaMeta.TENER -> "🎯 Qué quiero tener"
    CategoriaMeta.IR -> "🎯 Adónde quiero ir"
    CategoriaMeta.COMPARTIR -> "🎯 Qué deseo compartir"
    null -> "Sin categoría"
}

@Composable
fun GoalsListScreen(
    onMetaClick: (String) -> Unit,
    onNuevaMeta: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaMeta) { Text("+") }
        },
    ) { innerPadding ->
        if (!uiState.cargando && uiState.totalMetas == 0) {
            EmptyState(
                emoji = "🎯",
                titulo = "Todavía no tienes metas.",
                subtitulo = "Toca + para crear la primera, poco a poco se llega.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(text = "Tus metas", style = MaterialTheme.typography.titleLarge)
            if (uiState.totalMetas > 0) {
                Text(
                    text = "✅ ${uiState.totalCompletadas} de ${uiState.totalMetas} completadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                uiState.grupos.forEach { grupo ->
                    item {
                        Text(
                            text = "${etiquetaCategoria(grupo.categoria)} (${grupo.metas.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(grupo.metas, key = { it.id }) { meta ->
                        FilaMeta(meta, onClick = { onMetaClick(meta.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Fila compacta, pensada para repasar varias metas seguidas de un vistazo — antes tenía una
 * barra de progreso que "no llamaba la atención" (reportado por el usuario); ahora es un
 * contador tipo "(1/3)" a la derecha, junto a la fecha, bien separado del nombre. Ver
 * `Plan/08-decisiones-tecnicas.md`.
 */
@Composable
private fun FilaMeta(meta: MetaListItemUi, onClick: () -> Unit) {
    val completada = meta.fraccionProgreso >= 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = meta.nombre,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (completada) TextDecoration.LineThrough else null,
                color = if (completada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            meta.nombreHabitoVinculado?.let {
                Text(text = "Hábito: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (completada) "✅ " else "") + "(${meta.progreso.toInt()}/${meta.objetivo.toInt()})",
                style = MaterialTheme.typography.titleSmall,
                color = if (completada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            meta.fechaLimite?.let { fechaLimite ->
                Text(
                    text = DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaLimite)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
