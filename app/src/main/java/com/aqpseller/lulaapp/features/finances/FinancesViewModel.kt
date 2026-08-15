package com.aqpseller.lulaapp.features.finances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.usecase.finanzas.ObtenerBalanceMesUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinancesViewModel @Inject constructor(
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val obtenerBalanceMesUseCase: ObtenerBalanceMesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancesUiState())
    val uiState: StateFlow<FinancesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            obtenerBalanceMesUseCase(
                sesion.espacioId,
                DateTimeUtils.inicioDeMesEpochMillis(),
                DateTimeUtils.finDeMesEpochMillis(),
            )
                .map { movimientos -> movimientos.map { it.aUi() } }
                .collect { movimientosMes ->
                    val inicioHoy = DateTimeUtils.inicioDeHoyEpochMillis()
                    val finHoy = DateTimeUtils.finDeHoyEpochMillis()
                    _uiState.value = FinancesUiState(
                        cargando = false,
                        ingresosMes = movimientosMes.filter { it.tipo == TipoMovimientoFinanciero.INGRESO }.sumOf { it.monto },
                        gastosMes = movimientosMes.filter { it.tipo == TipoMovimientoFinanciero.EGRESO }.sumOf { it.monto },
                        ahorradoMes = movimientosMes
                            .filter { it.tipo == TipoMovimientoFinanciero.EGRESO && it.categoria.equals("Ahorro", ignoreCase = true) }
                            .sumOf { it.monto },
                        movimientosHoy = movimientosMes.filter { it.fecha in inicioHoy..finHoy },
                        movimientosMes = movimientosMes,
                    )
                }
        }
    }

    private fun com.aqpseller.lulaapp.domain.model.MovimientoFinanciero.aUi() = MovimientoUi(
        id = id,
        tipo = tipo,
        categoria = categoria,
        monto = monto,
        descripcion = descripcion,
        fecha = fecha,
    )
}
