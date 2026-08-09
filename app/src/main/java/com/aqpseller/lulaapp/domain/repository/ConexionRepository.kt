package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Conexion
import com.aqpseller.lulaapp.domain.model.TipoConexion
import kotlinx.coroutines.flow.Flow

interface ConexionRepository {
    fun observarConexionesDe(usuarioId: String): Flow<List<Conexion>>

    /** No duplica — si ya existe una conexión entre ambos (en cualquier sentido), no hace nada. */
    suspend fun crearSiNoExiste(usuarioA: String, usuarioB: String, tipo: TipoConexion, origenSolicitudId: String?)
}
