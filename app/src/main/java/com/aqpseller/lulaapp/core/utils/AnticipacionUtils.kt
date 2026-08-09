package com.aqpseller.lulaapp.core.utils

import com.aqpseller.lulaapp.domain.model.AnticipacionRecordatorio

/** Cuánto antes de la cita/control suena el recordatorio. */
fun anticipacionMillis(anticipacion: AnticipacionRecordatorio): Long = when (anticipacion) {
    AnticipacionRecordatorio.MISMO_DIA -> 0L
    AnticipacionRecordatorio.UN_DIA_ANTES -> 24 * 60 * 60_000L
    AnticipacionRecordatorio.UNA_SEMANA_ANTES -> 7 * 24 * 60 * 60_000L
}
