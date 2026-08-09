package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Nota
import kotlinx.coroutines.flow.Flow

interface NotaRepository {
    suspend fun crear(nota: Nota)
    suspend fun actualizar(nota: Nota, usuarioId: String)
    suspend fun eliminar(notaId: String, usuarioId: String)
    suspend fun obtenerPorId(notaId: String): Nota?
    fun observarPorEspacio(espacioId: String): Flow<List<Nota>>

    /** Para que una nota nueva aparezca primero — el menor `orden` existente menos uno, o 0 si no hay ninguna. */
    suspend fun obtenerOrdenParaNota(espacioId: String): Int

    suspend fun actualizarOrden(notaId: String, nuevoOrden: Int, usuarioId: String)
}
