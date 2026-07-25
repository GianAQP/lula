package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.AreaDeVida
import com.aqpseller.lulaapp.domain.model.Espacio
import com.aqpseller.lulaapp.domain.model.EspacioMiembro
import kotlinx.coroutines.flow.Flow

interface EspacioRepository {
    suspend fun contarEspacios(): Int
    suspend fun crearEspacioPersonal(espacio: Espacio, miembro: EspacioMiembro)
    suspend fun obtenerEspacioPersonal(usuarioId: String): Espacio?

    suspend fun contarAreasDeVida(): Int
    suspend fun sembrarAreasDeVida(areas: List<AreaDeVida>)
    fun observarAreasDeVidaActivas(): Flow<List<AreaDeVida>>
}
