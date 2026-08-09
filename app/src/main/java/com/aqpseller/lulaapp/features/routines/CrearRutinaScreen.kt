package com.aqpseller.lulaapp.features.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.ui.DescartarCambiosAlSalir
import com.aqpseller.lulaapp.core.ui.DictationTextField
import com.aqpseller.lulaapp.core.ui.etiquetaMomentoDelDia
import com.aqpseller.lulaapp.domain.model.MomentoDelDia

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CrearRutinaScreen(
    onGuardado: () -> Unit,
    onVerRutinas: () -> Unit,
    onSalirSinGuardar: () -> Unit = onGuardado,
    modifier: Modifier = Modifier,
    viewModel: CrearRutinaViewModel = hiltViewModel(),
) {
    var nombre by remember { mutableStateOf("") }
    var momento by remember { mutableStateOf(MomentoDelDia.MANANA) }
    var seleccionadas by remember { mutableStateOf(setOf<String>()) }

    fun snapshot() = listOf(nombre, momento, seleccionadas)
    var snapshotInicial by remember { mutableStateOf(snapshot()) }

    val guardado by viewModel.guardado.collectAsState()
    val formInicial by viewModel.formInicial.collectAsState()
    val actividadesDisponibles by viewModel.actividadesDisponibles.collectAsState()
    LaunchedEffect(guardado) { if (guardado) onGuardado() }
    DescartarCambiosAlSalir(
        hayContenidoSinGuardar = snapshotInicial != snapshot() && !guardado,
        onDescartar = onSalirSinGuardar,
    )
    LaunchedEffect(formInicial) {
        formInicial?.let {
            nombre = it.nombre
            momento = it.momentoDelDia
            seleccionadas = it.actividadesIncluidasIds.toSet()
        }
        snapshotInicial = snapshot()
    }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = if (viewModel.esEdicion) "Editar rutina" else "Nueva rutina", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onVerRutinas, modifier = Modifier.padding(top = 4.dp)) {
            Text("🧩 Ver mis rutinas")
        }
        Text(
            text = "Una rutina agrupa hábitos o tareas que ya tienes " +
                "para marcarlos juntos, de un solo toque (ej. \"Rutina de mañana\" = lavarme los " +
                "dientes + estirar + revisar la agenda). Para pagos periódicos (luz, agua) usa " +
                "una Tarea con repetición, no una rutina.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )

        DictationTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre (ej. Rutina de mañana)",
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(text = "Momento del día", modifier = Modifier.padding(top = 16.dp))
        FlowRow(modifier = Modifier.padding(top = 8.dp)) {
            MomentoDelDia.entries.forEach { opcion ->
                FilterChip(
                    selected = momento == opcion,
                    onClick = { momento = opcion },
                    label = { Text(etiquetaMomentoDelDia(opcion)) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                )
            }
        }

        Text(text = "¿Qué actividades agrupa?", modifier = Modifier.padding(top = 16.dp))
        if (actividadesDisponibles.isEmpty()) {
            Text(
                text = "Todavía no tienes hábitos ni tareas creados.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            FlowRow(modifier = Modifier.padding(top = 8.dp)) {
                actividadesDisponibles.forEach { actividad ->
                    FilterChip(
                        selected = actividad.id in seleccionadas,
                        onClick = {
                            seleccionadas = if (actividad.id in seleccionadas) {
                                seleccionadas - actividad.id
                            } else {
                                seleccionadas + actividad.id
                            }
                        },
                        label = { Text(actividad.nombre) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.guardar(nombre, momento, seleccionadas.toList()) },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(if (viewModel.esEdicion) "Guardar cambios" else "Crear")
        }
    }
}
