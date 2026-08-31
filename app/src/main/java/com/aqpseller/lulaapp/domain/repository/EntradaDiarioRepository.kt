package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.EntradaDiario
import kotlinx.coroutines.flow.Flow

interface EntradaDiarioRepository {
    suspend fun crear(entrada: EntradaDiario)
    suspend fun actualizar(entrada: EntradaDiario, usuarioId: String)
    suspend fun eliminar(entradaId: String, usuarioId: String)
    suspend fun obtenerPorId(entradaId: String): EntradaDiario?
    fun observarPorEspacio(espacioId: String): Flow<List<EntradaDiario>>

    /** Vacío (no todas) si `consulta` está en blanco — la pantalla decide qué mostrar en ese
     * caso. */
    fun buscar(espacioId: String, consulta: String): Flow<List<EntradaDiario>>
}
