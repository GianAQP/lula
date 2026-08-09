package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.ListaConItems
import com.aqpseller.lulaapp.domain.model.ListaEjecucion
import com.aqpseller.lulaapp.domain.model.ListaResumen
import kotlinx.coroutines.flow.Flow

interface ListaRepository {
    suspend fun crear(espacioId: String, nombre: String, itemsTexto: List<String>, usuarioId: String): String
    fun observarResumenPorEspacio(espacioId: String): Flow<List<ListaResumen>>
    fun observarConItems(listaId: String): Flow<ListaConItems?>
    suspend fun agregarItem(listaId: String, texto: String, usuarioId: String)
    suspend fun marcarItem(itemId: String, marcado: Boolean, usuarioId: String)
    suspend fun eliminarItem(itemId: String, usuarioId: String)
    /** Antes de desmarcar todo, guarda una foto de cómo quedaron los ítems — ver `observarHistorial`. */
    suspend fun reiniciar(listaId: String, usuarioId: String)
    suspend fun eliminarLista(listaId: String, usuarioId: String)
    fun observarHistorial(listaId: String): Flow<List<ListaEjecucion>>
}
