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
                    val enProgreso = metas.filter { it.fraccionProgreso < 1f }
                        .sortedBy { it.fechaLimite ?: Long.MAX_VALUE }
                    // Agrupadas por categoría — ver de un vistazo "8 metas cumplidas" (por
                    // categoría) motiva más que una sola lista plana (pedido del usuario). Las
                    // sin categoría van al final, como su propio grupo.
                    val completadas = metas.filter { it.fraccionProgreso >= 1f }
                    val categoriasOrdenadas = CategoriaMeta.entries + listOf(null)
                    val grupos = categoriasOrdenadas.mapNotNull { cat ->
                        completadas.filter { it.categoria == cat }
                            .takeIf { it.isNotEmpty() }
                            ?.let { GrupoMetasCompletadasUi(cat, it) }
                    }
                    _uiState.value = GoalsListUiState(cargando = false, metasEnProgreso = enProgreso, gruposCompletadas = grupos)
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
