package com.aqpseller.lulaapp.core.ui

import com.aqpseller.lulaapp.domain.model.EstadoActividad
import com.aqpseller.lulaapp.domain.model.TipoActividad

/** Un emoji por tipo, reusado en Hoy y Calendario — antes cada pantalla tenía su propia copia
 * de este mapeo (mismo riesgo de divergencia que ya pasó con el conteo de "hoy"). */
fun emojiTipoActividad(tipo: TipoActividad): String = when (tipo) {
    TipoActividad.HABITO -> "✅"
    TipoActividad.TAREA -> "📝"
    TipoActividad.MEDICAMENTO -> "💊"
    TipoActividad.CITA -> "📅"
    TipoActividad.FECHA_IMPORTANTE -> "🎉"
    else -> "🔔"
}

fun emojiEstadoActividad(estado: EstadoActividad): String = when (estado) {
    EstadoActividad.CONFIRMADO -> "✅"
    EstadoActividad.OMITIDO -> "⏭️"
    EstadoActividad.SIN_CONFIRMAR -> "⏳"
}
