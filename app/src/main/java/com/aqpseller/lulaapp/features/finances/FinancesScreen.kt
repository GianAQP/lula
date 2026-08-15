package com.aqpseller.lulaapp.features.finances

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.EmptyState
import com.aqpseller.lulaapp.core.ui.StatPill
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.ui.theme.LulaAsistenteContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaFinanzasContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaHabitoContainerLight
import com.aqpseller.lulaapp.ui.theme.LulaRachaContainerLight

@Composable
fun FinancesScreen(
    onAgregarGasto: () -> Unit,
    onAgregarIngreso: () -> Unit,
    onVerHistorial: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinancesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (!uiState.cargando && !uiState.hayAlgoRegistrado) {
        EmptyState(
            emoji = "💛",
            titulo = "Aún no has registrado nada.",
            subtitulo = "Registrar tus gastos te ayuda a ver a dónde va tu dinero, poco a poco.",
            textoBoton = "Registrar mi primer gasto",
            onBotonClick = onAgregarGasto,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Finanzas", style = MaterialTheme.typography.titleLarge)

        if (uiState.cargando) return

        Text(text = "Este mes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            StatPill(
                emoji = "📈",
                valor = "S/ ${"%.0f".format(uiState.ingresosMes)}",
                colorContenedor = LulaHabitoContainerLight,
                modifier = Modifier.padding(end = 8.dp),
            )
            StatPill(
                emoji = "📉",
                valor = "S/ ${"%.0f".format(uiState.gastosMes)}",
                colorContenedor = LulaRachaContainerLight,
                modifier = Modifier.padding(end = 8.dp),
            )
            StatPill(
                emoji = "⚖️",
                valor = "S/ ${"%.0f".format(uiState.balanceMes)}",
                colorContenedor = LulaFinanzasContainerLight,
            )
        }

        if (uiState.ahorradoMes > 0) {
            StatPill(
                emoji = "🐷",
                valor = "Ahorraste S/ ${"%.0f".format(uiState.ahorradoMes)} este mes",
                colorContenedor = LulaAsistenteContainerLight,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Text(text = "Hoy", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
        if (uiState.movimientosHoy.isEmpty()) {
            Text(text = "Nada registrado hoy.")
        } else {
            uiState.movimientosHoy.forEach { movimiento ->
                val signo = if (movimiento.tipo == TipoMovimientoFinanciero.EGRESO) "-" else "+"
                Text(text = "${movimiento.categoria}  $signo S/ ${"%.2f".format(movimiento.monto)}")
            }
            Text(text = "Neto de hoy: S/ ${"%.2f".format(uiState.netoHoy)}", style = MaterialTheme.typography.bodyLarge)
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            Button(onClick = onAgregarGasto, modifier = Modifier.padding(end = 8.dp)) {
                Text("+ Agregar gasto")
            }
            OutlinedButton(onClick = onAgregarIngreso) {
                Text("+ Agregar ingreso")
            }
        }

        Text(
            text = "Ver historial completo →",
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable(onClick = onVerHistorial),
        )
    }
}
