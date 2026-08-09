package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Meta
import kotlinx.coroutines.flow.Flow

interface MetaRepository {
    suspend fun crear(meta: Meta, usuarioId: String)
    suspend fun actualizar(meta: Meta, usuarioId: String)
    suspend fun eliminar(metaId: String, usuarioId: String)
    suspend fun agregarProgreso(metaId: String, incremento: Double, usuarioId: String)
    suspend fun obtenerConVinculo(metaId: String): Meta?
    fun observarPorEspacio(espacioId: String): Flow<List<Meta>>
}
