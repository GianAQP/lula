package com.aqpseller.lulaapp.domain.repository

import com.aqpseller.lulaapp.domain.model.Actividad
import com.aqpseller.lulaapp.domain.model.ActividadDetalle
import com.aqpseller.lulaapp.domain.model.EstadoActividad
import kotlinx.coroutines.flow.Flow

interface ActividadRepository {
    suspend fun crearHabito(actividad: Actividad, detalle: ActividadDetalle.Habito, usuarioId: String)
    suspend fun crearTarea(actividad: Actividad, detalle: ActividadDetalle.Tarea, usuarioId: String)
    suspend fun marcarEstado(id: String, estado: EstadoActividad, usuarioId: String)

    /** Trae actividades activas del espacio con su detalle (Habito/Tarea) ya resuelto. */
    fun observarActividadesDeEspacio(espacioId: String): Flow<List<Actividad>>
}
