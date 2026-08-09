package com.aqpseller.lulaapp.features.finances

import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import java.text.Normalizer

/**
 * Categorías predefinidas (ver `Plan/02-pantallas.md`: "Categoría: [selector]" — no estaba
 * implementado como selector real hasta ahora). "Ahorro" es una categoría de gasto más, no
 * una entidad aparte — ver nota en `Plan/08-decisiones-tecnicas.md` sobre por qué.
 */
object CategoriasFinanzas {
    const val OTROS = "Otros"

    val GASTO = listOf(
        "Comida", "Transporte", "Vivienda", "Servicios", "Salud",
        "Entretenimiento", "Educación", "Ropa", "Mascotas", "Ahorro", OTROS,
    )

    val INGRESO = listOf("Sueldo", "Extra/Freelance", "Regalo", OTROS)

    fun paraTipo(tipo: TipoMovimientoFinanciero): List<String> =
        if (tipo == TipoMovimientoFinanciero.EGRESO) GASTO else INGRESO

    /**
     * Si `texto` coincide con una categoría ya conocida (ignorando mayúsculas, tildes y
     * plural simple), devuelve esa categoría tal cual — así "comida", "Comida", "comidas"
     * dictadas por voz caen siempre en la misma categoría, en vez de crear variantes sueltas.
     */
    fun encajarEnConocida(texto: String, categoriasConocidas: List<String>): String {
        val normalizado = normalizar(texto)
        return categoriasConocidas.firstOrNull { normalizar(it) == normalizado } ?: texto.trim()
    }

    private fun normalizar(valor: String): String {
        val sinTildes = Normalizer.normalize(valor.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
        return sinTildes.removeSuffix("s")
    }
}
