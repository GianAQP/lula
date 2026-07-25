package com.aqpseller.lulaapp.navigation

object LulaDestinations {
    const val HOY = "hoy"
    const val PROXIMAMENTE = "proximamente/{titulo}"
    const val CREAR_HABITO = "crear_habito"
    const val CREAR_TAREA = "crear_tarea"
    const val CREAR_MOVIMIENTO = "crear_movimiento/{tipo}"
    const val CERRAR_DIA = "cerrar_dia"

    fun proximamente(titulo: String) = "proximamente/$titulo"
    fun crearMovimiento(tipo: String) = "crear_movimiento/$tipo"
}
