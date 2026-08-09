package com.aqpseller.lulaapp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Fila compacta "etiqueta — valor actual >" que abre un selector al tocarla, en vez de mostrar
 * todo el campo desplegado siempre — piloto del patrón pedido por el usuario (capturas de otra
 * app, ver `Plan/08-decisiones-tecnicas.md`), primero en Crear Medicamento.
 */
@Composable
fun SelectorRow(
    etiqueta: String,
    valor: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = etiqueta, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "$valor  ›",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
