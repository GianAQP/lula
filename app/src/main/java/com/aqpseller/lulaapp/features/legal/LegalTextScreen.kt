package com.aqpseller.lulaapp.features.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aqpseller.lulaapp.core.utils.DateTimeUtils

/**
 * Muestra el texto completo de uno de los 3 documentos legales (`TipoDocumentoLegal`). Para
 * Términos y Datos de salud, ofrece el botón "Aceptar" acá mismo — leer y aceptar en la misma
 * pantalla, en vez del atajo directo que tenía antes "Mi perfil" sin mostrar el texto. Política
 * de privacidad es de solo lectura porque hoy se acepta una sola vez, en la semilla inicial.
 */
@Composable
fun LegalTextScreen(
    modifier: Modifier = Modifier,
    viewModel: LegalTextViewModel = hiltViewModel(),
) {
    val aceptadoEn by viewModel.aceptadoEn.collectAsState()

    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text(text = viewModel.tipo.titulo, style = MaterialTheme.typography.titleLarge)

        Text(
            text = viewModel.texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp).weight(1f).verticalScroll(rememberScrollState()),
        )

        val fechaAceptado = aceptadoEn
        if (fechaAceptado != null) {
            Text(
                text = "✅ Aceptado el " +
                    DateTimeUtils.formatearFechaLarga(DateTimeUtils.epochMillisToLocalDate(fechaAceptado)),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else if (viewModel.permiteAceptar) {
            Button(
                onClick = viewModel::aceptar,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
            ) {
                Text("Aceptar")
            }
        }
    }
}
