package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.PropositoPersonal
import kotlinx.coroutines.flow.Flow

interface PropositoPersonalRepository {
    fun observar(espacioId: String): Flow<PropositoPersonal?>
    suspend fun guardarRespuesta(espacioId: String, propietario: String, preguntaId: String, respuesta: String)

    /** Borra solo esa respuesta, el resto queda intacto. */
    suspend fun eliminarRespuesta(espacioId: String, propietario: String, preguntaId: String)

    /** Borra todas las respuestas para empezar de cero. */
    suspend fun eliminarTodo(espacioId: String, propietario: String)
}
