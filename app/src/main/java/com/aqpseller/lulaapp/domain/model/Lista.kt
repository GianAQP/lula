package com.aqpseller.lulaapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Plantilla reutilizable de cosas por chequear (ej. "Lista de viaje") — a diferencia de Rutina,
 * sus ítems son texto libre, no Hábitos/Tareas existentes. "Reiniciar" desmarca todo para la
 * próxima vez que se use, sin duplicar la plantilla. Ver `Plan/08-decisiones-tecnicas.md`.
 */
data class Lista(
    val id: String,
    val nombre: String,
    val orden: Int,
)

data class ListaItem(
    val id: String,
    val listaId: String,
    val texto: String,
    val marcado: Boolean,
    val orden: Int,
)

data class ListaConItems(
    val id: String,
    val nombre: String,
    val items: List<ListaItem>,
)

data class ListaResumen(
    val id: String,
    val nombre: String,
    val total: Int,
    val marcados: Int,
    val orden: Int,
)

/** Foto de un ítem al momento de "Reiniciar lista" — para el historial de usos pasados. */
@Serializable
data class ItemListaSnapshot(
    val texto: String,
    val marcado: Boolean,
)

/** Un uso pasado de una Lista: qué había marcado justo antes de "Reiniciar". */
data class ListaEjecucion(
    val id: String,
    val listaId: String,
    val fecha: Long,
    val items: List<ItemListaSnapshot>,
) {
    val total: Int get() = items.size
    val marcados: Int get() = items.count { it.marcado }
}
