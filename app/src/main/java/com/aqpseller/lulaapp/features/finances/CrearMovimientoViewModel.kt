package com.aqpseller.lulaapp.features.finances

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqpseller.lulaapp.domain.model.TipoMovimientoFinanciero
import com.aqpseller.lulaapp.domain.usecase.finanzas.RegistrarMovimientoUseCase
import com.aqpseller.lulaapp.domain.usecase.usuario.ObtenerSesionActualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearMovimientoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val obtenerSesionActualUseCase: ObtenerSesionActualUseCase,
    private val registrarMovimientoUseCase: RegistrarMovimientoUseCase,
) : ViewModel() {

    val tipoInicial: TipoMovimientoFinanciero = savedStateHandle.get<String>("tipo")
        ?.let { runCatching { TipoMovimientoFinanciero.valueOf(it) }.getOrNull() }
        ?: TipoMovimientoFinanciero.EGRESO

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    fun guardar(tipo: TipoMovimientoFinanciero, monto: Double, categoria: String, descripcion: String?) {
        if (monto <= 0.0 || categoria.isBlank()) return
        viewModelScope.launch {
            val sesion = obtenerSesionActualUseCase()
            registrarMovimientoUseCase(
                espacioId = sesion.espacioId,
                usuarioId = sesion.usuarioId,
                tipo = tipo,
                monto = monto,
                categoria = categoria,
                descripcion = descripcion,
            )
            _guardado.value = true
        }
    }
}
