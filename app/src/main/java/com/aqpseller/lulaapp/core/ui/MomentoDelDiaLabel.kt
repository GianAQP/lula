package com.aqpseller.lulaapp.core.ui

import com.aqpseller.lulaapp.domain.model.MomentoDelDia

/**
 * "Mañana" a secas es ambiguo en español (momento del día vs. "tomorrow") — el usuario lo
 * reportó porque sonaba a que el hábito era para el día siguiente. "Por la mañana" deja claro
 * que es sobre la hora del día, no la fecha.
 */
fun etiquetaMomentoDelDia(momento: MomentoDelDia): String = when (momento) {
    MomentoDelDia.MANANA -> "Por la mañana"
    MomentoDelDia.TARDE -> "Por la tarde"
    MomentoDelDia.NOCHE -> "Por la noche"
}
