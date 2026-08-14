package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Agrupa una sección de pantalla en su propia tarjeta — reemplaza el patrón anterior de un solo
 * scroll separado únicamente por espacios, sin ninguna frontera visual entre temas distintos.
 * Nació en Ajustes (a pedido del usuario, mostrando capturas de otras apps como referencia) y se
 * reusa en Perfil por el mismo motivo. Ver `Plan/08-decisiones-tecnicas.md`.
 */
@Composable
fun TarjetaSeccion(titulo: String, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall)
            contenido()
        }
    }
}
