package com.aqpseller.lulaapp.core.utils

import java.util.UUID

/**
 * Toda entidad de Lula nace con un id UUID generado en el dispositivo, nunca autoincrement:
 * la app es local-first y las filas se crean offline antes de sincronizar (lección de Mayia,
 * donde ids basados en la posición de fila rompieron la sincronización).
 */
object IdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
}
