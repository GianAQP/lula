package com.aqpseller.lulaapp.features.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.CategoriaMeta
import com.aqpseller.lulaapp.domain.model.ComoSeMideMeta
import com.aqpseller.lulaapp.domain.usecase.actividad.ObtenerDetalleActividadUseCase
import com.aqpseller.lulaapp.domain.usecase.meta.MetaConProgreso
import com.aqpseller.lulaapp.domain.usecase.meta.ObtenerMetasConProgresoUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsListViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerMetasConProgresoUseCase: ObtenerMetasConProgresoUseCase,
    private val obtenerDetalleActividadUseCase: ObtenerDetalleActividadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsListUiState())
    val uiState: StateFlow<GoalsListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            obtenerMetasConProgresoUseCase(sesion.espacioId)
                .map { metas -> metas.map { it.aUi() } }
                .collect { metas ->
                    // Agrupadas por categoría (las 6 preguntas de ayuda), en progreso y
                    // completadas juntas — antes eran dos listas separadas (pendientes arriba,
                    // completadas agrupadas al final); el usuario pidió que todo se organice
                    // por categoría desde el principio, para repasar las metas rápido y
                    // seguidas. Dentro de cada grupo, pendientes primero (fecha más próxima
                    // arriba), completadas al final del mismo grupo.
                    val categoriasOrdenadas = CategoriaMeta.entries + listOf(null)
                    val grupos = categoriasOrdenadas.mapNotNull { cat ->
                        metas.filter { it.categoria == cat }
                            .sortedWith(compareBy({ it.fraccionProgreso >= 1f }, { it.fechaLimite ?: Long.MAX_VALUE }))
                            .takeIf { it.isNotEmpty() }
                            ?.let { GrupoMetasUi(cat, it) }
                    }
                    _uiState.value = GoalsListUiState(
                        cargando = false,
                        totalMetas = metas.size,
                        totalCompletadas = metas.count { it.fraccionProgreso >= 1f },
                        grupos = grupos,
                    )
                }
        }
    }

    private suspend fun MetaConProgreso.aUi(): MetaListItemUi {
        val habitoId = meta.actividadesVinculadasIds.firstOrNull()
        val nombreHabito = if (meta.comoSeMide == ComoSeMideMeta.POR_HABITO && habitoId != null) {
            obtenerDetalleActividadUseCase(habitoId)?.nombre
        } else {
            null
        }
        return MetaListItemUi(
            id = meta.id,
            nombre = meta.nombre,
            progreso = progreso,
            objetivo = meta.valorObjetivo,
            nombreHabitoVinculado = nombreHabito,
            fechaLimite = meta.fechaLimite,
            categoria = meta.categoria,
        )
    }
}
